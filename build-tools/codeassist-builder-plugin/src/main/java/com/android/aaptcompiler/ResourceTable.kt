package com.android.aaptcompiler

import com.android.aapt.Resources
import com.android.aaptcompiler.android.*
import com.android.resources.ResourceVisibility
import java.io.File
import java.util.*

/**
 * The container and index for all resources defined for a given app.
 */
class ResourceTable(val validateResources: Boolean = false, val logger: BlameLogger? = null) {
  /**
   * The string pool used by this resource table.
   */
  val stringPool = StringPool()

  /** The list of packages in this table. */
  val packages = mutableListOf<ResourceTablePackage>()

  /**
   * Set of dynamic packages that this table may reference.
   */
  private val includedPackages = mutableMapOf<Int, String>()

  enum class CollisionResult {
    KEEP_ORIGINAL,
    CONFLICT,
    TAKE_NEW
  }

  data class SearchResult(
    val tablePackage: ResourceTablePackage, val group: ResourceGroup, val entry: ResourceEntry)

  /**
   * Adds a resource to the table with the given value.
   */
  fun addResource(
    name: ResourceName, config: ConfigDescription, product: String, value: Value): Boolean =
    addResourceImpl(
      name, 0, config, product, value, ::resourceNameValidator, ::resolveValueCollision)


  /**
   * Adds a resource to the table with the given id and value.
   */
  fun addResourceWithId(
    name: ResourceName,
    id: Int,
    config: ConfigDescription,
    product: String,
    value: Value): Boolean =
    addResourceImpl(
      name, id, config, product, value, ::resourceNameValidator, ::resolveValueCollision)

  /**
   * Adds a file reference resource to the table with the given file path.
   */
  fun addFileReference(
    name: ResourceName,config: ConfigDescription, source: Source, path: String): Boolean =
    addFileReferenceImpl(
      name, config, source, path, null, ::resourceNameValidator)

  /**
   * Same as [addFileReference], but doesn't verify the name.
   */
  fun addFileReferenceMangled(
    name: ResourceName,
    config: ConfigDescription,
    source: Source,
    path: String,
    file: File): Boolean =
    addFileReferenceImpl(name, config, source, path, file, ::skipNameValidator)

  /**
   * Same as [addResource], but doesn't verify the name of the resource.
   */
  fun addResourceMangled(
    name: ResourceName, config: ConfigDescription, product: String, value: Value): Boolean =
    addResourceImpl(
      name, 0, config, product, value, ::skipNameValidator, ::resolveValueCollision)

  /**
   * Same as [addResourceWithId], but doesn't verify the name of the resource.
   */
  fun addResourceWithIdMangled(
    name: ResourceName,
    id: Int,
    config: ConfigDescription,
    product: String,
    value: Value) =
    addResourceImpl(
      name, id, config, product, value, ::skipNameValidator, ::resolveValueCollision)

  fun setVisibility(name: ResourceName, visibility: Visibility) =
    setVisibilityImpl(name, visibility, 0, ::resourceNameValidator)

  fun setVisibilityWithId(name: ResourceName, visibility: Visibility, id: Int) =
    setVisibilityImpl(name, visibility, id, ::resourceNameValidator)

  fun setVisibilityMangled(name: ResourceName, visibility: Visibility) =
    setVisibilityImpl(name, visibility, 0, ::skipNameValidator)

  fun setVisibilityWithIdMangled(name: ResourceName, visibility: Visibility, id: Int) =
    setVisibilityImpl(name, visibility, id, ::skipNameValidator)

  fun setAllowNew(name: ResourceName, allowNew: AllowNew) =
    setAllowNewImpl(name, allowNew, ::resourceNameValidator)

  fun setAllowNewMangled(name: ResourceName, allowNew: AllowNew) =
    setAllowNewImpl(name, allowNew, ::skipNameValidator)

  fun setOverlayable(name: ResourceName, overlayable: OverlayableItem) =
    setOverlayableImpl(name, overlayable, ::resourceNameValidator)

  fun setOverlayableMangled(name: ResourceName, overlayable: OverlayableItem) =
    setOverlayableImpl(name, overlayable, ::skipNameValidator)

  fun findResource(name: ResourceName): SearchResult? {
    val tablePackage = findPackage(name.pck!!)
    tablePackage ?: return null

    val group = tablePackage.findGroup(name.type)
    group ?: return null

    val entry = group.findEntry(name.entry!!)
    entry ?: return null

    return SearchResult(tablePackage, group, entry)
  }

  fun findPackage(name: String): ResourceTablePackage? {
    return packages.find { it.name == name }
  }

  fun findPackageById(id: Byte): ResourceTablePackage? {
    return packages.find { it.id == id }
  }

  fun createPackage(name: String, id: Byte = 0): ResourceTablePackage? {
    val tablePackage = findOrCreatePackage(name)

    if (id != 0.toByte()) {
      when {
        tablePackage.id == null -> {
          tablePackage.id = id
          return tablePackage
        }
        tablePackage.id != id -> return null
      }
    }

    return tablePackage
  }

  fun createPackageAllowingDuplicateNames(name: String, id: Byte): ResourceTablePackage {
    val match = packages.find { it.name == name && it.id == id }
    if (match != null) {
      return match
    }
    val newPackage = ResourceTablePackage(name, id)
    packages.add(newPackage)
    return newPackage
  }

  private fun findOrCreatePackage(name: String): ResourceTablePackage {
    val tablePackage = findPackage(name)
    return when (tablePackage) {
      null -> {
        val newPackage = ResourceTablePackage()
        newPackage.name = name
        packages.add(newPackage)
        newPackage
      }
      else -> tablePackage
    }
  }

  fun sort() {
    packages.sortWith(compareBy({it.name}, {it.id}))
    for (pkg in packages) {
      pkg.groups.sortWith(compareBy({it.type.ordinal}, {it.id}))
      for (group in pkg.groups) {
        for (entryByName in group.entries.values) {
          for (entry in entryByName.values) {
            entry.values.sortWith(compareBy({ it.config }, { it.product }))
          }
        }
      }
    }
  }

  private fun logError(source: Source?, message: String) {
    logger?.error(message, source)
  }

  private fun validateName(
    nameValidator: (String) -> String, name: ResourceName, source: Source): Boolean {
    val badCodePoint = nameValidator.invoke(name.entry!!)
    if (badCodePoint.isNotEmpty()) {
      logError(
        blameSource(source),
        "Resource '$name' has invalid entry name '${name.entry}'. " +
                "Invalid character '$badCodePoint'.")
      return false
    }
    return true
  }

  private fun addResourceImpl(
    name: ResourceName,
    id: Int,
    config: ConfigDescription,
    product: String,
    value: Value,
    nameValidator: (String) -> String,
    conflictResolver: (Value, Value) -> CollisionResult): Boolean {

    val source = value.source

    if (!validateName(nameValidator, name, source)) {
      return false
    }

    val tablePackage = findOrCreatePackage(name.pck!!)
    val packageId = tablePackage.id
    if (id.isValidDynamicId() && packageId != null && id.getPackageId() != packageId) {
      logError(
        value.source,
        "Failed to add resource '$name' with ID ${id.toString(16)} because " +
                "package '${tablePackage.name}' already has ID ${packageId.toString(16)}.")
      return false
    }

    val checkId = validateResources && id.isValidDynamicId()
    val useId = !validateResources && id.isValidDynamicId()

    val resourceGroup =
      tablePackage.findOrCreateGroup(name.type, if (useId) id.getTypeId() else null)
    val groupId = resourceGroup.id
    if (checkId && groupId != null && groupId != id.getTypeId()) {
      logError(
        value.source,
        "Failed to add resource '$name' with ID ${id.toString(16)} because type " +
                "'${resourceGroup.type.tagName}' already has ID ${groupId.toString(16)}.")
      return false
    }

    val resourceEntry =
      resourceGroup.findOrCreateEntry(name.entry!!, if (useId) id.getEntryId() else null)
    val entryId = resourceEntry.id
    if (checkId && entryId != null && id.getEntryId() != entryId) {
      logError(
        value.source,
        "Failed to add resource '$name' with ID ${id.toString(16)}, because resource already" +
                " has ID ${resourceIdFromParts(packageId!!, groupId!!, entryId).toString(16)}.")
      return false
    }

    val configValue = resourceEntry.findOrCreateValue(config, product)
    val oldValue = configValue.value
    if (oldValue == null) {
      // Resource does not exist, add it now.
      configValue.value = value
    } else {
      when (conflictResolver.invoke(oldValue, value)) {
        CollisionResult.TAKE_NEW -> configValue.value = value
        CollisionResult.CONFLICT -> {
          val previousSource =
            logger?.getOriginalSource(blameSource(oldValue.source)) ?: blameSource(oldValue.source)
          logError(
            value.source,
            "Duplicate value for resource '$name' with config '$config' and product '$product'. " +
                    "Resource was previously defined here: $previousSource.")
          return false
        }
        CollisionResult.KEEP_ORIGINAL -> {}
      }
    }

    if (id.isValidDynamicId()) {
      tablePackage.id = id.getPackageId()
      resourceGroup.id = id.getTypeId()
      resourceEntry.id = id.getEntryId()
    }

    return true
  }

  private fun addFileReferenceImpl(
    name: ResourceName,
    config: ConfigDescription,
    source: Source,
    path: String,
    file: File?,
    nameValidator: (String) -> String): Boolean {
    val fileReference = FileReference(stringPool.makeRef(path))
    fileReference.source = source
    fileReference.file = file
    return addResourceImpl(
      name, 0, config, "", fileReference, nameValidator, ::resolveValueCollision)
  }

  private fun setVisibilityImpl(
    name: ResourceName,
    visibility: Visibility,
    id: Int,
    nameValidator: (String) -> String) : Boolean {
    val source = visibility.source
    if (!validateName(nameValidator, name, source)) {
      return false
    }

    val tablePackage = findOrCreatePackage(name.pck!!)
    val packageId = tablePackage.id
    if (id.isValidDynamicId() && packageId != null && id.getPackageId() != packageId) {
      logError(
        source,
        "Failed to add resource '$name' with ID ${id.toString(16)} because package " +
                "'${tablePackage.name}' already has ID ${packageId.toString(16)}.")
      return false
    }

    val checkId = validateResources && id.isValidDynamicId()
    val useId = !validateResources && id.isValidDynamicId()

    val resourceGroup =
      tablePackage.findOrCreateGroup(name.type, if(useId) id.getTypeId() else null)
    val groupId = resourceGroup.id
    if (checkId && groupId != null && groupId != id.getTypeId()) {
      logError(
        source,
        "Failed to add resource '$name' with ID ${id.toString(16)} because type " +
                "'${resourceGroup.type.tagName}' already has ID ${groupId.toString(16)}.")
      return false
    }

    val resourceEntry =
      resourceGroup.findOrCreateEntry(name.entry!!, if (useId) id.getEntryId() else null)
    val entryId = resourceEntry.id
    if (checkId && entryId != null && id.getEntryId() != entryId) {
      logError(
        source,
        "Failed to add resource '$name' with ID ${id.toString(16)}, because resource already " +
                "has ID ${resourceIdFromParts(packageId!!, groupId!!, entryId).toString(16)}.")
      return false
    }

    if (id.isValidDynamicId()) {
      tablePackage.id = id.getPackageId()
      resourceGroup.id = id.getTypeId()
      resourceEntry.id = id.getEntryId()
    }

    if (visibility.level == ResourceVisibility.PUBLIC) {
      resourceGroup.visibility = ResourceVisibility.PUBLIC
    }

    when {
      visibility.level == ResourceVisibility.UNDEFINED &&
        resourceEntry.visibility.level != ResourceVisibility.UNDEFINED -> {
      }
      visibility.level == ResourceVisibility.PRIVATE &&
        resourceEntry.visibility.level == ResourceVisibility.PUBLIC -> {
        logError(
                source,
                "Failed to add resource '$name' as private (java-symbol) because it was " +
                        "previously defined as public.")
        return false
      }
      visibility.level == ResourceVisibility.PUBLIC &&
        resourceEntry.visibility.level == ResourceVisibility.PRIVATE -> {
        logError(
                source,
                "Failed to add resource '$name' as public because it was previously defined as " +
                        "private (java-symbol).")
        return false
      }
      else -> {
        resourceEntry.visibility = visibility
      }
    }

    return true
  }

  private fun setAllowNewImpl(
    name: ResourceName, allowNew: AllowNew, nameValidator: (String) -> String): Boolean {
    if (!validateName(nameValidator, name, allowNew.source)) {
      return false
    }

    val tablePackage = findOrCreatePackage(name.pck!!)
    val group = tablePackage.findOrCreateGroup(name.type)
    val entry = group.findOrCreateEntry(name.entry!!)
    entry.allowNew = allowNew
    return true
  }

  private fun setOverlayableImpl(
    name: ResourceName, overlayable: OverlayableItem, nameValidator: (String) -> String): Boolean {
    if (!validateName(nameValidator, name, overlayable.source)) {
      return false
    }

    val tablePackage = findOrCreatePackage(name.pck!!)
    val group = tablePackage.findOrCreateGroup(name.type)
    val entry = group.findOrCreateEntry(name.entry!!)

    val oldEntry = entry.overlayable
    if (oldEntry != null) {
      val previousSource =
        logger?.getOriginalSource(blameSource(oldEntry.source)) ?: blameSource(oldEntry.source)
      logError(
        overlayable.source,
        "Failed to add overlayable declaration for resource '$name', because resource already " +
                "has an overlayable defined here: $previousSource.")
      return false
    }
    entry.overlayable = overlayable
    return true
  }

  companion object {
    fun resolveValueCollision(existing: Value, incoming: Value):CollisionResult {
      val existingAttr = existing as? AttributeResource
      val incomingAttr = incoming as? AttributeResource

      incomingAttr ?: return when {
        incoming.weak -> CollisionResult.KEEP_ORIGINAL
        existing.weak -> CollisionResult.TAKE_NEW
        else -> CollisionResult.CONFLICT
      }

      existingAttr ?: return when {
        existing.weak -> CollisionResult.TAKE_NEW
        else -> CollisionResult.CONFLICT
      }

      if (existingAttr.isCompatibleWith(incomingAttr)) {
        return if (existingAttr.weak) CollisionResult.TAKE_NEW else CollisionResult.KEEP_ORIGINAL
      }

      if (existingAttr.weak && existingAttr.typeMask == Resources.Attribute.FormatFlags.ANY_VALUE) {
        return CollisionResult.TAKE_NEW
      }

      if (incomingAttr.weak && incomingAttr.typeMask == Resources.Attribute.FormatFlags.ANY_VALUE) {
        return CollisionResult.KEEP_ORIGINAL
      }

      return CollisionResult.CONFLICT
    }

    fun resourceNameValidator(name: String): String =
      if (isValidResourceEntryName(name)) "" else name

    fun skipNameValidator(name: String): String {
      return ""
    }
  }
}

/** the public status of a resource */
class Visibility(
  val source: Source = Source(""),
  val comment: String = "",
  val level: ResourceVisibility = ResourceVisibility.UNDEFINED)

class AllowNew (
  val source: Source,
  val comment: String)

class Overlayable (
  val name: String,
  val actor: String,
  val source: Source) {

  constructor(): this("","", Source(""))

  companion object {
    const val ACTOR_SCHEME = "overlay"
    const val ACTOR_SCHEME_URI = "$ACTOR_SCHEME://"
  }

  override fun equals(other: Any?): Boolean {
    if (other is Overlayable) {
      return name == other.name && actor == other.actor
    }
    return false
  }
}

class OverlayableItem(
  val overlayable: Overlayable,
  val policies: Int = Policy.NONE,
  val comment: String = "",
  val source: Source = Source("")) {

  override fun equals(other: Any?): Boolean {
    if (other is OverlayableItem) {
      return overlayable == other.overlayable && policies == other.policies
    }
    return false
  }

  object Policy {
    const val NONE = 0
    const val PUBLIC = 1 shl 0
    const val SYSTEM = 1 shl 1
    const val VENDOR = 1 shl 2
    const val PRODUCT = 1 shl 3
    const val SIGNATURE = 1 shl 4
    const val ODM = 1 shl 5
    const val OEM = 1 shl 6
  }
}

class ResourceTablePackage(var name: String = "", var id: Byte? = null) {

  internal val groups = mutableListOf<ResourceGroup>()

  fun findGroup(type: AaptResourceType, groupId: Byte? = null) =
    if (groupId != null) {
      groups.find {type == it.type && groupId == it.id} ?:
        groups.find {type == it.type && it.id == null}
    } else {
      groups.find {type == it.type}
    }

  fun findOrCreateGroup(type: AaptResourceType, groupId: Byte? = null): ResourceGroup {
    val group = findGroup(type, groupId)
    return when(group) {
      null -> {
        val newGroup = ResourceGroup(type)
        newGroup.id = groupId
        groups.add(newGroup)
        newGroup
      }
      else -> group
    }
  }
}

class ResourceGroup(val type : AaptResourceType) {
  var id: Byte? = null
  var visibility = ResourceVisibility.UNDEFINED

  internal val entries = sortedMapOf<String, SortedMap<Short?, ResourceEntry>>()

  internal fun getStyleable(entry: Map.Entry<String, SortedMap<Short?, ResourceEntry>>): Styleable {
      val mapEntry = entry.value
      val styleableContainer = mapEntry.values.first().values
      if (styleableContainer.size != 1)
          error("Too many resources in one entry: ${styleableContainer.size}")
      return styleableContainer[0].value!! as Styleable
  }

  fun findEntry(name: String, entryId: Short? = null): ResourceEntry? {
    val nameGroup = entries[name] ?: return null
    return if (entryId != null) {
      nameGroup[entryId] ?: nameGroup[null]
    } else {
      nameGroup[nameGroup.firstKey()]
    }
  }

  fun findOrCreateEntry(name: String, entryId: Short? = null): ResourceEntry {
    val entry = findEntry(name, entryId)
    return when(entry) {
      null -> {
        val newEntry = ResourceEntry(name)
        newEntry.id = entryId
        entries.getOrPut(name) { TreeMap(nullsFirst()) }[entryId] = newEntry
        newEntry
      }
      else -> entry
    }
  }
}

class ResourceEntry(val name : String) {
  var id: Short? = null
  var visibility = Visibility(Source(""), "", ResourceVisibility.UNDEFINED)

  var allowNew: AllowNew? = null
  var overlayable: OverlayableItem? = null

  internal val values = mutableListOf<ResourceConfigValue>()

  fun findValue(config: ConfigDescription, product: String = ""): ResourceConfigValue? {
    return values.find { it.config == config && it.product == product }
  }

  fun findOrCreateValue(config: ConfigDescription, product: String = ""): ResourceConfigValue {
    val configValue = findValue(config, product)
    return when (configValue) {
      null -> {
        val newConfigValue = ResourceConfigValue(config, product)
        values.add(newConfigValue)
        newConfigValue
      }
      else -> configValue
    }
  }
}

data class ResourceConfigValue(
  val config: ConfigDescription,
  val product : String,
  var value: Value? = null)
