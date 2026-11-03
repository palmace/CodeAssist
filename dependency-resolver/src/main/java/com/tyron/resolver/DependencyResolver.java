package com.tyron.resolver;

import com.tyron.resolver.model.Dependency;
import com.tyron.resolver.model.Pom;
import com.tyron.resolver.repository.RepositoryManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DependencyResolver {

    private final RepositoryManager repository;
    private final Map<String, Pom> resolvedPoms; // Key: groupId:artifactId

    private ResolveListener mListener;

    public DependencyResolver(RepositoryManager repository) {
        this.repository = repository;
        this.resolvedPoms = new HashMap<>();
    }

    public void setResolveListener(ResolveListener listener) {
        mListener = listener;
    }

    public interface ResolveListener {
        void onResolve(String message);
        void onFailure(String message);
    }

    public List<Pom> resolveDependencies(List<Dependency> declaredDependencies) {
        resolvedPoms.clear();
        List<Pom> initialPoms = new ArrayList<>();
        
        for (Dependency dependency : declaredDependencies) {
            if (mListener != null) {
                mListener.onResolve("Resolviendo dependencia declarada: " + dependency);
            }

            // El manager ya se encarga de buscar en caché o red con Escudo de Salud
            Pom pom = repository.getPom(dependency.toString());
            if (pom != null) {
                pom.setExcludes(dependency.getExcludes());
                pom.setUserDefined(true);
                initialPoms.add(pom);
            } else if (mListener != null) {
                mListener.onFailure("ERROR: No se pudo encontrar el POM para: " + dependency + 
                                  ". Verifica tu conexión o los repositorios configurados.");
            }
        }
        return resolve(initialPoms);
    }

    public List<Pom> resolve(List<Pom> declaredDependencies) {
        for (Pom pom : declaredDependencies) {
            resolve(pom);
        }
        return new ArrayList<>(resolvedPoms.values());
    }

    private void resolve(Pom pom) {
        String key = pom.getGroupId() + ":" + pom.getArtifactId();
        
        // LÓGICA DE RESOLUCIÓN DE CONFLICTOS DE VERSIONES (OMNI)
        if (resolvedPoms.containsKey(key)) {
            Pom existing = resolvedPoms.get(key);
            
            // Si la nueva versión es superior a la ya resuelta, la reemplazamos
            int versionComparison = compareVersions(existing.getVersionName(), pom.getVersionName());
            if (versionComparison >= 0) {
                // La versión existente es mayor o igual. La mantenemos.
                return; 
            }
            
            if (mListener != null) {
                mListener.onResolve("Conflicto detectado en " + key + ": Actualizando " + 
                                  existing.getVersionName() + " -> " + pom.getVersionName());
            }
            resolvedPoms.remove(key);
        }

        if (mListener != null) {
            mListener.onResolve("Analizando transitivas de: " + pom.getArtifactId() + ":" + pom.getVersionName());
        }

        // Registrar antes de la recursión para evitar bucles infinitos
        resolvedPoms.put(key, pom);

        List<Dependency> excludes = pom.getExcludes();

        for (Dependency dependency : pom.getDependencies()) {
            // Ignorar scopes que no afectan al runtime de la app
            String scope = dependency.getScope();
            if ("test".equals(scope) || "provided".equals(scope) || "system".equals(scope)) {
                continue;
            }

            // Aplicar exclusiones (Propagación del Escudo)
            boolean isExcluded = excludes.stream().filter(Objects::nonNull).anyMatch(ex -> 
                Objects.equals(ex.getGroupId(), dependency.getGroupId()) &&
                Objects.equals(ex.getArtifactId(), dependency.getArtifactId())
            );

            if (isExcluded) {
                if (mListener != null) mListener.onResolve("Excluyendo: " + dependency.getArtifactId());
                continue;
            }

            Pom depPom = repository.getPom(dependency.toString());
            if (depPom == null) {
                // Si falla una transitiva, intentamos reportarlo pero no detenemos todo si es opcional
                if (!dependency.isOptional() && mListener != null) {
                    mListener.onFailure("Advertencia: No se pudo resolver transitiva crítica: " + dependency);
                }
                continue;
            }

            if (!depPom.equals(pom)) {
                // Propagar las exclusiones hacia abajo en el árbol
                depPom.addExcludes(excludes);
                resolve(depPom);
            }
        }
    }

    /**
     * Compara dos versiones usando ComparableVersion de forma segura
     */
    private int compareVersions(String v1, String v2) {
        try {
            ComparableVersion version1 = new ComparableVersion(v1);
            ComparableVersion version2 = new ComparableVersion(v2);
            return version1.compareTo(version2);
        } catch (Exception e) {
            // Fallback: Comparación de texto si algo falla
            return v1.compareTo(v2);
        }
    }
}
