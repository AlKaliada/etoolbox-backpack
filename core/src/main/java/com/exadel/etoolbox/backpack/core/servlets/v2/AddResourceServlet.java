package com.exadel.etoolbox.backpack.core.servlets.v2;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PathModel;
import com.exadel.etoolbox.backpack.core.util.RequestUtils;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/add/path",
                "sling.servlet.paths=/services/backpack/add/list",
                "sling.servlet.paths=/services/backpack/add/query",
                "sling.servlet.paths=/services/backpack/add/children",
                "sling.servlet.paths=/services/backpack/add/liveCopies",
                "sling.servlet.paths=/services/backpack/add/references",
                "sling.servlet.methods=post"
        })
@SuppressWarnings("PackageAccessibility")
public class AddResourceServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();

    private static final String SERVLET_PATH_BASE = "services/backpack/add/";

    private static final String APPLICATION_JSON = "application/json";

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient BaseResourceService baseResourceService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {

        response.setContentType(APPLICATION_JSON);

        Map<String, String[]> parameterMap = RequestUtils
                .modifyParameterMap(request.getParameterMap(), "type", StringUtils.substringAfter(request.getPathInfo(), SERVLET_PATH_BASE));

        ValidatorResponse<PathModel> validatorResponse = requestAdapter.adaptValidate(parameterMap, PathModel.class);

        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {

            final PackageInfo packageInfo = baseResourceService.getPackageInfo(
                    request.getResourceResolver(),
                    validatorResponse.getModel()
            );

            response.getWriter().write(GSON.toJson(packageInfo));

            if (!PackageStatus.MODIFIED.equals(packageInfo.getPackageStatus())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            }
        }
    }
}
