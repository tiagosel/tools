import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Teste {
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Exemplo de JSON de entrada
        String inputJsonString = """
{
  "user": {
    "id": "456",
    "name": "Ana Silva",
    "details": {
      "age": "29",
      "is_active": "true",
      "address": {
        "street": "Av. Paulista",
        "number": "1000",
        "city": "São Paulo"
      }
    }
  },
  "orders": [
    {
      "order_id": "12345",
      "total_price": "250.75",
      "items": [
        { "item_id": "A1", "name": "Laptop", "quantity": "1", "price": "250.75" }
      ]
    },
    {
      "order_id": "67890",
      "total_price": "100.50",
      "items": [
        { "item_id": "B2", "name": "Mouse", "quantity": "2", "price": "50.25" },
        { "item_id": "C3", "name": "Keyboard", "quantity": "1", "price": "100.50" }
      ]
    }
  ],
  "preferences": {
    "newsletter": "true",
    "theme": "light"
  },
  "y":{
   "x": [
    {
      "order_id": "12345",
      "total_price": "250.75",
      "items": [
        { "item_id": "A1", "name": "Laptop", "quantity": "1", "price": "250.75" }
      ]
    }]}
  
}
        """;

        // Template de mapeamento
        String mappingTemplateString = """
{
  "UserId": "{{user.id}}",
  "UserName": "{{user.name}}",
  "UserAge": "{{user.details.age}}",
  "UserStatus": "{{user.details.is_active}}",
  "Address": {
    "Street": "{{user.details.address.street}}",
    "Number": "{{user.details.address.number}}",
    "City": "{{user.details.address.city}}"
  },
  "Orders": [
    {
      "OrderId": "{{orders[*].order_id}}"
    }
  ],
  "NewsletterSubscribed": "{{preferences.newsletter}}",
  "ThemePreference": "{{preferences.theme}}"
}
        """;

        // Especificação de tipos
        String fieldSpecString = """
{
  "user.id": { "type": "string", "convert_to": "integer", "required": true },
  "user.name": { "type": "string", "required": true },
  "user.details.age": { "type": "string", "convert_to": "integer", "required": false },
  "user.details.is_active": { "type": "string", "convert_to": "boolean", "required": true },
  "user.details.address.street": { "type": "string", "required": true },
  "user.details.address.number": { "type": "string", "convert_to": "integer", "required": true },
  "user.details.address.city": { "type": "string", "required": true },
  "orders.order_id": { "type": "string", "convert_to": "integer", "required": true },
  "orders.total_price": { "type": "string", "convert_to": "double", "required": true },
  "orders.items.item_id": { "type": "string", "required": true },
  "orders.items.name": { "type": "string", "required": true },
  "orders.items.quantity": { "type": "string", "convert_to": "integer", "required": true },
  "orders.items.price": { "type": "string", "convert_to": "double", "required": true },
  "preferences.newsletter": { "type": "string", "convert_to": "boolean", "required": true },
  "preferences.theme": { "type": "string", "required": true }
}
        """;

        try {
            JsonNode inputJson = objectMapper.readTree(inputJsonString);
            JsonNode mappingTemplate = objectMapper.readTree(mappingTemplateString);
            JsonNode fieldSpec = objectMapper.readTree(fieldSpecString);

            JsonNode transformedJson = JsonMapperUtil.transform(inputJson, mappingTemplate, fieldSpec);

            // Exibir JSON transformado
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformedJson));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}