/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.backpack.core.servlets.model;

import com.exadel.etoolbox.backpack.request.annotations.RequestMapping;
import com.exadel.etoolbox.backpack.request.annotations.RequestParam;
import com.exadel.etoolbox.backpack.request.annotations.Validate;
import com.exadel.etoolbox.backpack.request.validator.impl.RequiredValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Represents the set of user-defined options for a path filter creation.
 * Upon initialization, used as part of {@link PackageModel}
 */
@RequestMapping
public class PathModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathModel.class);

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Path field is required")
    private String packagePath;

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Payload is required")
    private List<String> payload;

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Type is required")
    private String type;

    public PathModel() {
    }

    public PathModel(final String packagePath, final List<String> payload, final String type) {
        this.packagePath = packagePath;
        this.payload = payload;
        this.type = type;
    }

    @PostConstruct
    @SuppressWarnings("PackageAccessibility") // because PostConstruct class reported as a non-bundle dependency
    private void init() {
        if (StringUtils.isNotBlank(packagePath)) {
            try {
                packagePath = URLDecoder.decode(packagePath, StandardCharsets.UTF_8.displayName());
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Path decode exception", e);
            }
        }
    }

    public List<String> getPayload() {
        return payload;
    }

    public String getType() {
        return type;
    }

    public String getPackagePath() {
        return packagePath;
    }
}
