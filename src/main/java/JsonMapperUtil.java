import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonMapperUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode transform(JsonNode inputJson, JsonNode mappingTemplate, JsonNode fieldSpec) {
        ObjectNode outputJson = objectMapper.createObjectNode();

        mappingTemplate.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isTextual()) {
                processTextualField(inputJson, outputJson, key, value.asText(), fieldSpec);
            } else if (value.isObject()) {
                processObjectField(inputJson, outputJson, key, value, fieldSpec);
            } else if (value.isArray()) {
                processArrayField(inputJson, outputJson, key, value, fieldSpec);
            }
        });

        return outputJson;
    }

    private static void processTextualField(JsonNode inputJson, ObjectNode outputJson, String key, String jsonPath, JsonNode fieldSpec) {
        List<JsonNode> extractedValues = extractJsonValueWithWildcard(inputJson, jsonPath);
        if (!extractedValues.isEmpty()) {
            Object transformedValue = applyFieldSpec(jsonPath, extractedValues.get(0), fieldSpec);
            if (transformedValue != null) {
                outputJson.putPOJO(key, transformedValue);
            }
        }
    }

    private static void processObjectField(JsonNode inputJson, ObjectNode outputJson, String key, JsonNode objectTemplate, JsonNode fieldSpec) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectTemplate.fields().forEachRemaining(entry -> {
            String fieldKey = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            if (fieldValue.isTextual()) {
                processTextualField(inputJson, objectNode, fieldKey, fieldValue.asText(), fieldSpec);
            } else if (fieldValue.isArray()) {
                processArrayField(inputJson, objectNode, fieldKey, fieldValue, fieldSpec);
            }
        });
        outputJson.set(key, objectNode);
    }

    private static void processArrayField(JsonNode inputJson, ObjectNode outputJson, String key, JsonNode arrayTemplate, JsonNode fieldSpec) {
        String arrayPath = arrayTemplate.get(0).fields().next().getValue().asText();
        List<JsonNode> arrayNodes = extractJsonValueWithWildcard(inputJson, arrayPath);

        if (!arrayNodes.isEmpty()) {
            List<JsonNode> processedNodes = arrayNodes.stream()
                    .map(item -> createArrayObject(item, arrayTemplate, fieldSpec))
                    .collect(Collectors.toList());
            outputJson.set(key, objectMapper.valueToTree(processedNodes));
        } else {
            throw new IllegalArgumentException("Caminho do array não encontrado ou não é um array: " + arrayPath);
        }
    }

    private static ObjectNode createArrayObject(JsonNode item, JsonNode arrayTemplate, JsonNode fieldSpec) {
        ObjectNode arrayObject = objectMapper.createObjectNode();
        arrayTemplate.get(0).fields().forEachRemaining(innerEntry -> {
            String field = innerEntry.getKey();
            JsonNode fieldValue = innerEntry.getValue();

            if (fieldValue.isTextual()) {  // Apenas processamos se for texto (caminho JSON válido)
                String fieldPath = fieldValue.asText();
                //List<JsonNode> fieldValues = extractJsonValueWithWildcard(item, fieldPath);

//                arrayObject.put("teste", item);

//                if (!fieldValues.isEmpty()) {
                Object transformedField = applyFieldSpec(fieldPath, item, fieldSpec);
                if (transformedField != null) {
                    arrayObject.putPOJO(field, transformedField);
//                    }
                }
            }
        });
        return arrayObject;
    }

    private static List<JsonNode> extractJsonValueWithWildcard(JsonNode inputJson, String jsonPath) {
        String[] pathParts = jsonPath.replace("{{", "").replace("}}", "").split("\\.");
        List<JsonNode> results = new ArrayList<>();
        extractJsonValueRecursive(inputJson, pathParts, 0, results);
        return results;
    }

    private static void extractJsonValueRecursive(JsonNode currentNode, String[] pathParts, int index, List<JsonNode> results) {
        if (index >= pathParts.length) {
            results.add(currentNode);
            return;
        }

        String part = pathParts[index];

        if (part.contains("[*]")) {
            String cleanPart = part.replace("[*]", "");
            if (currentNode.has(cleanPart) && currentNode.get(cleanPart).isArray()) {
                for (JsonNode subNode : currentNode.get(cleanPart)) {
                    extractJsonValueRecursive(subNode, pathParts, index + 1, results);
                }
            }
        } else {
            if (currentNode.has(part)) {
                extractJsonValueRecursive(currentNode.get(part), pathParts, index + 1, results);
            }
        }
    }

    private static Object applyFieldSpec(String jsonPath, JsonNode value, JsonNode fieldSpec) {
        String cleanPath = jsonPath.replace("{", "").replace("}", "");
        JsonNode spec = fieldSpec.get(cleanPath);
        if (spec == null || value == null) return value;

        String targetType = spec.has("convert_to") ? spec.get("convert_to").asText() : "string";
        boolean required = spec.has("required") && spec.get("required").asBoolean();

        if (required && (value == null || value.asText().isEmpty())) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + cleanPath);
        }

        return convertValue(value, targetType);
    }

    private static Object convertValue(JsonNode value, String targetType) {
        if (value == null) return null;

        return switch (targetType) {
            case "integer" -> value.asInt();
            case "boolean" -> value.asBoolean();
            case "double" -> value.asDouble();
            default -> value.asText();
        };
    }
}