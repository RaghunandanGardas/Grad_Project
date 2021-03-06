package Pwd.src.Services;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import Pwd.src.Cryptographic_Functions.AES;
import Pwd.src.DBConnect.Constants_PWD;
import Pwd.src.DBConnect.MongoDBConnect;
import Pwd.src.Models.Field;

public class UserService {
	private static MongoDBConnect mongoDBConnect;
	private static MongoDatabase mongoDatabase;

	public UserService() {
		mongoDBConnect = new MongoDBConnect();
		mongoDatabase = mongoDBConnect.getMongoDatabase();
	}

	private int userType = 2;

	public List<Field> getAllFields(String userId) {
		// TODO Auto-generated method stub
		MongoCollection<Document> collection = mongoDatabase.getCollection(userId);
		List<Field> userFields = new ArrayList<Field>();
		MongoCursor<Document> cursor = collection.find().iterator();
		try {
			while (cursor.hasNext()) {
				Document document = cursor.next();
				Field field = new Field();
				field.setFieldId(document.getString(Constants_PWD.fieldId));
				field.setFieldDecrypted(AES.decrypt(document.getString(Constants_PWD.fieldEncrypted),
						userId.substring(0, 4) + field.getFieldId().substring(0, 4)));
				field.setFieldName(document.getString(Constants_PWD.fieldName));
				userFields.add(field);
			}
			return userFields;
		} catch (Exception ex) {
			System.out.println("Exception in GetFields -> userService." + ex.toString());
			return null;
		} finally {
			// mongoDBConnect.CloseDB();
		}

	}

	public List<String> getFieldIds(String userId) {
		MongoCollection<Document> collection = mongoDatabase.getCollection(userId);
		List<String> fieldIds = new ArrayList<String>();
		MongoCursor<Document> cursor = collection.find().iterator();
		try {
			while (cursor.hasNext()) {
				Document document = cursor.next();
				String fieldId;
				fieldId = document.getString(Constants_PWD.fieldId);
				fieldIds.add(fieldId);
			}
			return fieldIds;
		} catch (Exception ex) {
			System.out.println("Exception in GetFields -> userService." + ex.toString());
			return null;
		} finally {
			// mongoDBConnect.CloseDB();
		}
	}

	public boolean addField(String userId, Field userField) throws Exception {

		try {
			MongoCollection<Document> collection = mongoDatabase.getCollection(userId);
			// String encryptedString = AES.encrypt(originalString, secretKey);
			String secretKey = userId.substring(0, 4) + userField.getFieldId().substring(0, 4);
			userField.setFieldEncrypted(AES.encrypt(userField.getFieldDecrypted(), secretKey));
			System.out.println("Encrypted is " + userField.getFieldEncrypted());
			userField.setFieldDecrypted("");
			Gson gson = new Gson();
			String json = gson.toJson(userField);
			// Parse to bson document and insert
			Document doc = Document.parse(json);
			collection.insertOne(doc);
			return true;
		} catch (MongoWriteException mx) {
			System.out.println("Duplicate FieldId's cannot exist. Please change FieldId");
			throw mx;
			// return false;
		} catch (Exception ex) {
			System.out.println("Exception while adding Field in userService." + ex.toString());
			ex.printStackTrace();
			throw ex;
		} finally {
		}

	}

	public List<Field> modifyField(String userId, Field userField) {
		try {
			MongoCollection<Document> collection = mongoDatabase.getCollection(userId);
			BasicDBObject fields = new BasicDBObject();
			System.out.println(userField.getFieldId());
			fields.put(Constants_PWD.fieldId, userField.getFieldId());
			FindIterable<Document> document = collection.find(fields);
			MongoCursor<Document> cursor = document.iterator();
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String secretKey = userId.substring(0, 4) + ((String) doc.get(Constants_PWD.fieldId)).substring(0, 4);
				String newEncryptedString = AES.encrypt(userField.getFieldDecrypted(), secretKey);
				BasicDBObject update = new BasicDBObject();
				update.append("$set", new BasicDBObject().append(Constants_PWD.fieldEncrypted, newEncryptedString));
				UpdateResult updateResult=collection.updateOne(fields, update);
				System.out.println("Modified count is "+updateResult.getModifiedCount());
			}
			return getAllFields(userId);
		} catch (Exception ex) {
			System.out.println("Exception in modifyField->userServer:" + ex.toString());
		}
		return getAllFields(userId);
	}

	public boolean deleteField(String userId, String fieldId) {
		try {
			MongoCollection<Document> collection = mongoDatabase.getCollection(userId);
			BasicDBObject document = new BasicDBObject();
			document.put("number", 2);
			Bson filters = Filters.eq(Constants_PWD.fieldId, fieldId);
			DeleteResult deleteResult = collection.deleteOne(Filters.eq("fieldId", fieldId));
			if (deleteResult.getDeletedCount() > 0) {
				System.out.println(deleteResult.getDeletedCount());
				return true;
			} else {
				return false;
			}
		} catch (Exception ex) {
			System.out.println(ex.toString());
			return false;
		}
	}

}
