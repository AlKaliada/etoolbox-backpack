package com.exadel.etoolbox.backpack.core.model;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PathInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Model(adaptables = SlingHttpServletRequest.class)
public class PackageEntriesDatasource {

    private static final String PAGE_ENTRY_TYPE = "page";

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService
    private PackageInfoService packageInfoService;

    @OSGiService
    private BasePackageService basePackageService;

    @OSGiService
    private RequestAdapter requestAdapter;

    private String resourceType;

    @PostConstruct
    private void initModel() {

        List<Resource> resources = new ArrayList<>();

        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);

        if (validatorResponse.isValid() && packageInfoService.packageExists(request.getResourceResolver(), validatorResponse.getModel())) {
            PackageInfo packageInfo = packageInfoService.getPackageInfo(request.getResourceResolver(), validatorResponse.getModel().getPackagePath());

            resourceType = Optional.ofNullable(request.getResource().getChild("datasource"))
                    .map(Resource::getValueMap)
                    .map(vm -> vm.get("itemResourceType", String.class))
                    .orElse(JcrConstants.NT_UNSTRUCTURED);

            if (packageInfo.getPaths() != null) {
                packageInfo.getPaths().forEach(path -> {
                    PathInfo pathInfo = packageInfo.getPathInfo(path);
                    if (pathInfo == null) {
                        resources.add(createPackageEntry(PAGE_ENTRY_TYPE, path, null));
                        return;
                    }

                    List<Resource> subsidiaries = new ArrayList<>();

                    addSubsidiaries(subsidiaries, pathInfo.getTags(), "tag");
                    addSubsidiaries(subsidiaries, pathInfo.getPages(), PAGE_ENTRY_TYPE);
                    addSubsidiaries(subsidiaries, pathInfo.getAssets(), "asset");
                    addSubsidiaries(subsidiaries, pathInfo.getLiveCopies(), "liveCopy");

                    if (subsidiaries.isEmpty()) {
                        resources.add(createPackageEntry(PAGE_ENTRY_TYPE, path, null));
                    } else {
                        resources.add(createPackageEntry(PAGE_ENTRY_TYPE, path, null, subsidiaries)
                        );
                    }
                });
            }
        }
        List<Resource> filteredResources = filterResources(resources);
        request.setAttribute(DataSource.class.getName(), new SimpleDataSource(filteredResources.iterator()));
    }

    private Resource createPackageEntry(String type, String title, Map<String, Object> additionalProperties) {
        return createPackageEntry(type, title, additionalProperties, null);
    }

    private Resource createPackageEntry(String type, String title, Map<String, Object> additionalProperties, List<Resource> children) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_TITLE, title);
        properties.put("type", type);
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
        ValueMap valueMap = new ValueMapDecorator(properties);
        return new ValueMapResource(
                request.getResourceResolver(),
                "/package-entry/" + title.replace("/", "-"),
                resourceType,
                valueMap,
                children);
    }

    private void addSubsidiaries(List<Resource> subsidiaries, Set<String> subsidiaryPaths, String type) {
        if (!subsidiaryPaths.isEmpty()) {
            subsidiaries.addAll(subsidiaryPaths.stream()
                    .map(path -> createPackageEntry(type, path, ImmutableMap.of("upstream", path)))
                    .collect(Collectors.toList()));
        }
    }

    private List<Resource> filterResources(List<Resource> resources) {
        List<String> childrenPaths = resources.stream()
                .flatMap(resource -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(resource.getChildren().iterator(), 0), false))
                .map(Resource::getPath)
                .collect(Collectors.toList());
        return resources.stream().filter(resource -> !childrenPaths.contains(resource.getPath())).collect(Collectors.toList());
    }
}
