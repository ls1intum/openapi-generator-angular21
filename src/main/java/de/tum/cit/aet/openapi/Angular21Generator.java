/*
 * Copyright (c) 2024 TUM Applied Education Technologies (AET)
 * Licensed under the MIT License
 */
package de.tum.cit.aet.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.TypeScriptAngularClientCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAPI Generator for Angular 21 with modern best practices:
 * <ul>
 *   <li>Signal-based httpResource for GET requests</li>
 *   <li>Injectable services with inject() function for mutations</li>
 *   <li>Standalone services (providedIn: 'root')</li>
 *   <li>Strict TypeScript with readonly modifiers</li>
 * </ul>
 *
 * @author TUM AET
 */
public class Angular21Generator extends TypeScriptAngularClientCodegen {

    private static final Logger LOGGER = LoggerFactory.getLogger(Angular21Generator.class);

    /** Generator name used by the OpenAPI Generator SPI and CLI. */
    public static final String GENERATOR_NAME = "angular21";
    /** Config option for enabling httpResource-based GET resources. */
    public static final String USE_HTTP_RESOURCE = "useHttpResource";
    /** Config option for enabling inject() instead of constructor injection. */
    public static final String USE_INJECT_FUNCTION = "useInjectFunction";
    /** Config option for generating separate resource files for GET operations. */
    public static final String SEPARATE_RESOURCES = "separateResources";
    /** Config option for adding readonly modifiers to response models. */
    public static final String READONLY_MODELS = "readonlyModels";

    /** Whether to generate httpResource-based GET resources. */
    protected boolean useHttpResource = true;
    /** Whether to use Angular inject() for service dependencies. */
    protected boolean useInjectFunction = true;
    /** Whether to place GET resources in separate files. */
    protected boolean separateResources = true;
    /** Whether to add readonly modifiers to response models. */
    protected boolean readonlyModels = true;

    /** Creates a configured Angular 21 generator with default options. */
    public Angular21Generator() {
        super();

        // Override template directory
        embeddedTemplateDir = templateDir = GENERATOR_NAME;

        // Set output folder structure
        outputFolder = "generated-code" + File.separator + GENERATOR_NAME;

        // Configure model and API naming
        modelTemplateFiles.clear();
        modelTemplateFiles.put("model.mustache", ".ts");

        apiNameSuffix = "Api";
        apiTemplateFiles.clear();
        apiTemplateFiles.put("api-service.mustache", "-api.ts");

        // Add resource templates for GET operations
        supportingFiles.clear();

        // CLI options
        cliOptions.add(new CliOption(USE_HTTP_RESOURCE,
                "Use httpResource for GET requests (signal-based reactive fetching)")
                .defaultValue("true"));
        cliOptions.add(new CliOption(USE_INJECT_FUNCTION,
                "Use inject() function instead of constructor injection")
                .defaultValue("true"));
        cliOptions.add(new CliOption(SEPARATE_RESOURCES,
                "Generate separate resource files for GET operations")
                .defaultValue("true"));
        cliOptions.add(new CliOption(READONLY_MODELS,
                "Add readonly modifier to model properties")
                .defaultValue("true"));
    }

    @Override
    public String getName() {
        return GENERATOR_NAME;
    }

    @Override
    public String getHelp() {
        return "Generates Angular 21 client code with modern best practices including " +
                "httpResource for GET requests, inject() function, and signal-based reactivity.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // Replace base generator supporting files with our template set only.
        supportingFiles.clear();

        // Process custom options
        if (additionalProperties.containsKey(USE_HTTP_RESOURCE)) {
            useHttpResource = Boolean.parseBoolean(additionalProperties.get(USE_HTTP_RESOURCE).toString());
        }
        additionalProperties.put(USE_HTTP_RESOURCE, useHttpResource);

        if (additionalProperties.containsKey(USE_INJECT_FUNCTION)) {
            useInjectFunction = Boolean.parseBoolean(additionalProperties.get(USE_INJECT_FUNCTION).toString());
        }
        additionalProperties.put(USE_INJECT_FUNCTION, useInjectFunction);

        if (additionalProperties.containsKey(SEPARATE_RESOURCES)) {
            separateResources = Boolean.parseBoolean(additionalProperties.get(SEPARATE_RESOURCES).toString());
        }
        additionalProperties.put(SEPARATE_RESOURCES, separateResources);

        if (additionalProperties.containsKey(READONLY_MODELS)) {
            readonlyModels = Boolean.parseBoolean(additionalProperties.get(READONLY_MODELS).toString());
        }
        additionalProperties.put(READONLY_MODELS, readonlyModels);

        // Add resource template if enabled
        if (useHttpResource && separateResources) {
            apiTemplateFiles.put("api-resource.mustache", "-resources.ts");
        }

        // Update supporting files

        LOGGER.info("Angular21 Generator initialized with: useHttpResource={}, useInjectFunction={}, " +
                "separateResources={}, readonlyModels={}",
                useHttpResource, useInjectFunction, separateResources, readonlyModels);
    }

    @Override
    public void processOpenAPI(OpenAPI openAPI) {
        super.processOpenAPI(openAPI);

        if (openapiGeneratorIgnoreList == null) {
            openapiGeneratorIgnoreList = new HashSet<>();
        }

        Map<String, TagUsage> usageByTag = new HashMap<>();
        if (openAPI != null && openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                if (pathItem == null) {
                    return;
                }
                addOperationUsage(pathItem.getGet(), true, usageByTag);
                addOperationUsage(pathItem.getPost(), false, usageByTag);
                addOperationUsage(pathItem.getPut(), false, usageByTag);
                addOperationUsage(pathItem.getDelete(), false, usageByTag);
                addOperationUsage(pathItem.getPatch(), false, usageByTag);
                addOperationUsage(pathItem.getHead(), false, usageByTag);
                addOperationUsage(pathItem.getOptions(), false, usageByTag);
                addOperationUsage(pathItem.getTrace(), false, usageByTag);
            });
        }

        for (Map.Entry<String, TagUsage> entry : usageByTag.entrySet()) {
            String apiFilename = toApiFilename(entry.getKey());
            TagUsage usage = entry.getValue();
            if (!usage.hasMutation) {
                openapiGeneratorIgnoreList.add("api/" + apiFilename + "-api.ts");
            }
            if (useHttpResource && separateResources && !usage.hasGet) {
                openapiGeneratorIgnoreList.add("api/" + apiFilename + "-resources.ts");
            }
        }
    }

    @Override
    public String toModelFilename(String name) {
        // Use kebab-case for filenames without .model suffix (new Angular style guide)
        return toKebabCase(name);
    }

    @Override
    public String toApiFilename(String name) {
        // Use kebab-case for API files
        return toKebabCase(name);
    }

    @Override
    public String toApiName(String name) {
        return StringUtils.camelize(name) + "Api";
    }

    @Override
    public String toOperationId(String operationId) {
        String name = super.toOperationId(operationId);
        String normalized = name.replaceFirst("^_+", "");
        normalized = normalized.replaceFirst("\\d+$", "");
        if (normalized.isBlank()) {
            normalized = "operation";
        }
        return normalized;
    }

    private void addOperationUsage(Operation operation, boolean isGet, Map<String, TagUsage> usageByTag) {
        if (operation == null) {
            return;
        }

        List<String> tags = operation.getTags();
        if (tags == null || tags.isEmpty()) {
            tags = Collections.singletonList("default");
        }
        for (String tag : tags) {
            String sanitizedTag = sanitizeTag(tag);
            TagUsage usage = usageByTag.computeIfAbsent(sanitizedTag, key -> new TagUsage());
            if (isGet) {
                usage.hasGet = true;
            } else {
                usage.hasMutation = true;
            }
        }
    }

    private static final class TagUsage {
        private boolean hasGet;
        private boolean hasMutation;
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        Map<String, ModelsMap> result = super.postProcessAllModels(objs);

        // Add readonly modifier info to properties
        for (ModelsMap modelsMap : result.values()) {
            for (ModelMap modelMap : modelsMap.getModels()) {
                CodegenModel model = modelMap.getModel();

                // Mark whether this is a Create/Update DTO (should not have readonly)
                boolean isInputDto = model.name.endsWith("Create") ||
                        model.name.endsWith("Update") ||
                        model.name.endsWith("Request") ||
                        model.name.endsWith("Input");

                model.vendorExtensions.put("x-is-input-dto", isInputDto);
                model.vendorExtensions.put("x-use-readonly", readonlyModels && !isInputDto);

                // Process properties
                for (CodegenProperty property : model.vars) {
                    property.vendorExtensions.put("x-is-readonly", readonlyModels && !isInputDto);
                }
            }
        }

        return result;
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationsMap result = super.postProcessOperationsWithModels(objs, allModels);

        OperationMap operations = result.getOperations();
        List<CodegenOperation> ops = operations.getOperation();

        // Separate GET operations from mutations
        List<CodegenOperation> getOperations = new ArrayList<>();
        List<CodegenOperation> mutationOperations = new ArrayList<>();

        for (CodegenOperation op : ops) {
            // Add custom vendor extensions
            op.vendorExtensions.put("x-use-inject", useInjectFunction);

            if ("GET".equalsIgnoreCase(op.httpMethod)) {
                op.vendorExtensions.put("x-is-get", true);
                op.vendorExtensions.put("x-use-http-resource", useHttpResource);
                getOperations.add(op);
            } else {
                op.vendorExtensions.put("x-is-get", false);
                op.vendorExtensions.put("x-is-mutation", true);
                mutationOperations.add(op);
            }

            // Process path parameters
            processPathParameters(op);

            // Process query parameters
            processQueryParameters(op);

            // Build URL path templates without relying on the default Configuration encoder.
            String pathTemplate = buildPathTemplate(op, false);
            String resourcePathTemplate = buildPathTemplate(op, true);
            op.vendorExtensions.put("xPathTemplate", pathTemplate);
            op.vendorExtensions.put("xResourcePathTemplate", resourcePathTemplate);
            if (pathTemplate != null && !pathTemplate.isBlank()) {
                op.path = pathTemplate;
            }
        }

        // Add to vendor extensions for template access
        operations.put("getOperations", getOperations);
        operations.put("mutationOperations", mutationOperations);
        operations.put("hasGetOperations", !getOperations.isEmpty());
        operations.put("hasMutationOperations", !mutationOperations.isEmpty());

        return result;
    }

    /**
     * Process path parameters for the operation.
     */
    private void processPathParameters(CodegenOperation op) {
        if (op.pathParams != null) {
            for (CodegenParameter param : op.pathParams) {
                // Convert to camelCase for TypeScript
                param.vendorExtensions.put("x-ts-name", toCamelCase(param.paramName));
                param.vendorExtensions.put("x-is-numeric", isNumericParam(param));
            }
        }
    }

    /**
     * Process query parameters for the operation.
     */
    private void processQueryParameters(CodegenOperation op) {
        if (op.queryParams != null && !op.queryParams.isEmpty()) {
            op.vendorExtensions.put("x-has-query-params", true);

            // Generate interface name for query params
            String paramsInterfaceName = toPascalCase(op.operationId) + "Params";
            op.vendorExtensions.put("x-params-interface-name", paramsInterfaceName);

            for (CodegenParameter param : op.queryParams) {
                param.vendorExtensions.put("x-ts-name", toCamelCase(param.paramName));
            }
        } else {
            op.vendorExtensions.put("x-has-query-params", false);
        }
    }

    /**
     * Build a URL path template that encodes path params without using Configuration.
     */
    private String buildPathTemplate(CodegenOperation op, boolean useSignalValue) {
        if (op.path == null) {
            return null;
        }

        String rawPath = unescapeHtmlEntities(op.path);
        Map<String, String> signalValueByParamName = new HashMap<>();
        Map<String, String> templateVarByParamName = new HashMap<>();
        if (useSignalValue && op.pathParams != null) {
            for (CodegenParameter param : op.pathParams) {
                Object tsName = param.vendorExtensions.get("x-ts-name");
                String baseName = tsName != null ? tsName.toString() : param.paramName;
                signalValueByParamName.put(param.paramName, baseName + "Value");
            }
        }
        if (op.pathParams != null) {
            for (CodegenParameter param : op.pathParams) {
                Object tsName = param.vendorExtensions.get("x-ts-name");
                String baseName = tsName != null ? tsName.toString() : param.paramName;
                boolean isNumeric = Boolean.TRUE.equals(param.vendorExtensions.get("x-is-numeric"));
                if (useSignalValue) {
                    templateVarByParamName.put(param.paramName, isNumeric ? baseName + "Value" : baseName + "Path");
                } else {
                    templateVarByParamName.put(param.paramName, isNumeric ? baseName : baseName + "Path");
                }
            }
        }

        Pattern pattern = Pattern.compile("\\$\\{this\\.configuration\\.encodeParam\\([^)]*?value: ([^,}]+)[^)]*\\)\\}");
        Matcher matcher = pattern.matcher(rawPath);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String valueVar = matcher.group(1).trim();
            String replacementVar = valueVar;
            if (templateVarByParamName.containsKey(valueVar)) {
                replacementVar = templateVarByParamName.get(valueVar);
            } else if (useSignalValue && signalValueByParamName.containsKey(valueVar)) {
                replacementVar = signalValueByParamName.get(valueVar);
            }
            String replacement = "${" + replacementVar + "}";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        String path = buffer.toString();
        if (op.pathParams != null) {
            for (CodegenParameter param : op.pathParams) {
                Object tsName = param.vendorExtensions.get("x-ts-name");
                String baseName = tsName != null ? tsName.toString() : param.paramName;
                String valueVar = useSignalValue ? baseName + "Value" : baseName;
                if (templateVarByParamName.containsKey(param.paramName)) {
                    valueVar = templateVarByParamName.get(param.paramName);
                }
                String placeholder = "{" + param.baseName + "}";
                path = path.replace(placeholder, "${" + valueVar + "}");
            }
        }

        return path;
    }

    private String unescapeHtmlEntities(String value) {
        return value.replace("&quot;", "\"").replace("&#39;", "'");
    }

    private boolean isNumericParam(CodegenParameter param) {
        if (Boolean.TRUE.equals(param.isInteger) || Boolean.TRUE.equals(param.isNumber)) {
            return true;
        }
        if ("number".equals(param.dataType) || "number".equals(param.baseType) || "integer".equals(param.baseType)) {
            return true;
        }
        return false;
    }

    /**
     * Convert string to kebab-case.
     */
    private String toKebabCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .toLowerCase();
    }

    /**
     * Convert string to camelCase.
     */
    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        // Handle snake_case and kebab-case
        Pattern pattern = Pattern.compile("[-_]([a-zA-Z0-9])");
        Matcher matcher = pattern.matcher(name);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(buffer);
        String result = buffer.toString();
        // Ensure first character is lowercase
        return Character.toLowerCase(result.charAt(0)) + result.substring(1);
    }

    /**
     * Convert string to PascalCase.
     */
    private String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + "api";
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + "models";
    }
}
