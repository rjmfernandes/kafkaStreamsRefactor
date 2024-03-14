package io.confluent.developer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import shoes.shoe_customers;
import shoes.shoe_product;

import java.util.HashMap;
import java.util.Map;

public class MongodbUpsert {

    public static final String CONNECTION_STRING = "mongodb://root:example@mongo:27017/";
    public static final String DB = "demo";
    public static final String COLLECTION = "orders2";
    MongoClient mongoClient;
    MongoDatabase database;

    public MongodbUpsert(){
        String connectionString = CONNECTION_STRING;
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(DB);
    }

    public void upsertCustomer(shoe_customers customer) {

        // Access specific collection within the database
        MongoCollection<Document> collection = database.getCollection(COLLECTION);

        // Define the document to upsert
        Document query = new Document("customer.id", customer.getId());
        Bson update = Updates.combine(Updates.set("customer", getCustomerMap(customer)));
        // Instructs the driver not to insert a new document if none match the query
        UpdateOptions options = new UpdateOptions().upsert(false);

        collection.updateMany(query, update,options);

        System.out.println("Upserted orders for customer " + customer.getId()+" :"+customer);

    }

    public void upsertProduct(shoe_product product) {

        // Access specific collection within the database
        MongoCollection<Document> collection = database.getCollection(COLLECTION);

        // Define the document to upsert
        Document query = new Document("product.id", product.getId());
        Bson update = Updates.combine(Updates.set("customer", getProductMap(product)));
        // Instructs the driver not to insert a new document if none match the query
        UpdateOptions options = new UpdateOptions().upsert(false);

        collection.updateMany(query, update,options);

        System.out.println("Upserted orders for product " + product.getId()+" :"+product);

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
