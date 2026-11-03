package org.gradle.internal.resource;

import com.google.common.hash.HashCode;
import org.gradle.api.resources.MissingResourceException;
import org.gradle.internal.DisplayName;
import org.gradle.api.resources.ResourceException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * A {@code Resource} that has text content.
 */
public interface TextResource extends Resource {
    ResourceLocation getLocation();
    DisplayName getLongDisplayName();
    DisplayName getShortDisplayName();

    @Nullable
    File getFile();

    @Nullable
    Charset getCharset();

    boolean isContentCached();
    boolean getExists() throws ResourceException;
    boolean getHasEmptyContent() throws ResourceException;
    Reader getAsReader() throws ResourceException;
    String getText() throws ResourceException;
    HashCode getContentHash() throws ResourceException;
}
