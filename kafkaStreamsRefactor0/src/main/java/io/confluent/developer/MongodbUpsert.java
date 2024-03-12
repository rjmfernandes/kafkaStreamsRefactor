package io.confluent.developer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import shoes.shoe_customers;
import shoes.shoe_orders_customers_products;
import shoes.shoe_product;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MongodbUpsert {

    MongoClient mongoClient;
    MongoDatabase database;

    public MongodbUpsert(){
        String connectionString = "mongodb://root:example@mongo:27017/";
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("demo");
    }

    public static void main(String[] args) {
        MongodbUpsert mongodbUpsert = new MongodbUpsert();
        shoe_orders_customers_products order = new shoe_orders_customers_products();
        order.setOrderId(1);
        order.setTs(Instant.now());
        shoe_customers customer = new shoe_customers();
        customer.setId("1");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("doe@test.com");
        customer.setPhone("123456789");
        customer.setStreetAddress("123 Main St");
        customer.setState("CA");
        customer.setZipCode("12345");
        customer.setCountry("USA");
        customer.setCountryCode("US");
        order.setCustomer(customer);
        shoe_product product = new shoe_product();
        product.setId("1");
        product.setName("shoe");
        product.setBrand("nike");
        product.setSalePrice(100);
        product.setRating(4.5);
        order.setProduct(product);
        mongodbUpsert.upsert(order);
        mongodbUpsert.close();
    }

    public void upsert(shoe_orders_customers_products order) {

        // Access specific collection within the database
        MongoCollection<Document> collection = database.getCollection("orders");

        // Define the document to upsert
        Document query = new Document("_id", order.getOrderId());
        Document update = new Document("$set", buildDocument(order));

        // Perform upsert operation
        UpdateOptions options = new UpdateOptions().upsert(true);
        collection.updateOne(query, update,options);

        System.out.println("Upserted order " + order.getOrderId()+" :"+order);

    }

    private Document buildDocument(shoe_orders_customers_products order) {
        Document document= new Document("_id",order.getOrderId());
        document.append("ts", order.getTs());
        document.append("customer",getCustomerMap(order.getCustomer()));
        document.append("product",getProductMap(order.getProduct()));
        return document;
    }

    private static Map<String,Object> getProductMap(shoe_product product) {
        Map<String,Object> productMap = new HashMap<String,Object>();
        productMap.put("id",product.getId());
        productMap.put("name",product.getName());
        productMap.put("brand",product.getBrand());
        productMap.put("sale_price",product.getSalePrice());
        productMap.put("rating",product.getRating());
        return productMap;
    }

    private static Map<String,Object> getCustomerMap(shoe_customers customer) {
        Map<String,Object> customerMap = new HashMap<String,Object>();
        customerMap.put("id",customer.getId());
        customerMap.put("firstName",customer.getFirstName());
        customerMap.put("lastName",customer.getLastName());
        customerMap.put("email",customer.getEmail());
        customerMap.put("phone",customer.getPhone());
        customerMap.put("street_address",customer.getStreetAddress());
        customerMap.put("state",customer.getState());
        customerMap.put("zip_code",customer.getZipCode());
        customerMap.put("country",customer.getCountry());
        customerMap.put("country_code",customer.getCountryCode());
        return customerMap;
    }

    public void close(){
        mongoClient.close();
    }
}
