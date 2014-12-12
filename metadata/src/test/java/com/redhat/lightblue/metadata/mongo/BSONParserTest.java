package com.redhat.lightblue.metadata.mongo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.redhat.lightblue.metadata.Enum;
import com.redhat.lightblue.metadata.EnumValue;
import com.redhat.lightblue.metadata.Enums;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.metadata.parser.PropertyParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.test.metadata.parser.FakeDataStoreParser;

public class BSONParserTest {

    private BSONParser parser;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setup() {
        Extensions<BSONObject> extensions = new Extensions<>();
        extensions.addDefaultExtensions();
        extensions.registerDataStoreParser("empty", new FakeDataStoreParser<BSONObject>("empty"));
        extensions.registerPropertyParser("fake", new PropertyParser<BSONObject>() {
            @Override
            public Object parse(String name, MetadataParser<BSONObject> p, BSONObject node) {
                p.getStringProperty(node, "answer");
                return 42;
            }

            @Override
            public void convert(MetadataParser<BSONObject> p, BSONObject parent, Object object) {
                BSONObject t = p.newNode();
                p.putObject(parent, "fake", t);
                p.putString(t, "answer", object.toString());
            }
        });
        parser = new BSONParser(extensions, new DefaultTypes());
    }

    @After
    public void tearDown() {
        parser = null;
    }

    @Test
    public void testConvertEnums() throws IOException{
        String enumName = "FakeEnum";
        String enumValue1 = "FakeEnumValue1";
        String enumValue2 = "FakeEnumValue2";

        Enum e = new Enum(enumName);
        e.setValues(new HashSet<EnumValue>(Arrays.asList(
                new EnumValue(enumName, enumValue1, null),
                new EnumValue(enumName, enumValue2, null))));

        Enums enums = new Enums();
        enums.addEnum(e);

        BSONObject enumsNode = parser.newNode();
        parser.convertEnums(enumsNode, enums);

        String bsonString = enumsNode.toString();
        Assert.assertTrue(bsonString.contains("enums"));
        Assert.assertTrue(bsonString.contains(enumName));
        Assert.assertTrue(bsonString.matches(".*\"values\" : \\[.*"));
        //Should just be string elements, not a complex objects.
        Assert.assertTrue(bsonString.contains(enumValue1));
        Assert.assertFalse(bsonString.contains("\"name\" : \"" + enumValue1 + "\""));
        Assert.assertTrue(bsonString.contains(enumValue2));
        Assert.assertFalse(bsonString.contains("\"name\" : \"" + enumValue2 + "\""));
    }

    @Test
    public void testConvertEnums_WithDescription() throws IOException{
        String enumName = "FakeEnum";
        String enumValue1 = "FakeEnumValue1";
        String enumDescription1 = "this is a fake description of enum value 1";
        String enumValue2 = "FakeEnumValue2";
        String enumDescription2 = "this is a fake description of enum value 2";
        String enumValue3 = "FakeEnumWithoutDescription";

        Enum e = new Enum(enumName);
        e.setValues(new HashSet<EnumValue>(Arrays.asList(
                new EnumValue(enumName, enumValue1, enumDescription1),
                new EnumValue(enumName, enumValue2, enumDescription2),
                new EnumValue(enumName, enumValue3, null))));

        Enums enums = new Enums();
        enums.addEnum(e);

        BSONObject enumsNode = parser.newNode();
        parser.convertEnums(enumsNode, enums);

        String bsonString = enumsNode.toString();
        Assert.assertTrue(bsonString.contains("enums"));
        Assert.assertTrue(bsonString.contains(enumName));
        Assert.assertTrue(bsonString.matches(".*\"annotatedValues\" : \\[.*"));
        Assert.assertTrue(bsonString.contains("{ \"name\" : \"" + enumValue1 + "\" , \"description\" : \"" + enumDescription1 + "\"}"));
        Assert.assertTrue(bsonString.contains("{ \"name\" : \"" + enumValue2 + "\" , \"description\" : \"" + enumDescription2 + "\"}"));
        //Should just be a string element, not a complex object.
        Assert.assertTrue(bsonString.contains(enumValue3));
        Assert.assertFalse(bsonString.contains("\"name\" : \"" + enumValue3 + "\""));
    }

    @Test
    public void testParseEnum() throws IOException{
        String enumName = "FakeEnum";
        String enumValue1 = "FakeEnumValue1";
        String enumValue2 = "FakeEnumValue2";

        BSONObject enumsNode = parser.newNode();
        enumsNode.put("name", enumName);
        enumsNode.put("values", Arrays.asList(enumValue1, enumValue2));

        Enum e = parser.parseEnum(enumsNode);

        Assert.assertEquals(enumName, e.getName());
        Set<EnumValue> values = e.getEnumValues();
        Assert.assertEquals(2, values.size());
        Assert.assertTrue(values.contains(new EnumValue(e.getName(), enumValue1, null)));
        Assert.assertTrue(values.contains(new EnumValue(e.getName(), enumValue2, null)));
    }

    @Test
    public void testParseEnum_WithDescriptions() throws IOException{
        String enumName = "FakeEnum";
        String enumValue1 = "FakeEnumValue1";
        String enumDescription1 = "this is a fake description of enum value 1";
        String enumValue2 = "FakeEnumValue2";
        String enumDescription2 = "this is a fake description of enum value 2";

        BSONObject enum1Node = parser.newNode();
        enum1Node.put("name", enumValue1);
        enum1Node.put("description", enumDescription1);
        BSONObject enum2Node = parser.newNode();
        enum2Node.put("name", enumValue2);
        enum2Node.put("description", enumDescription2);

        BSONObject enumsNode = parser.newNode();
        enumsNode.put("name", enumName);
        enumsNode.put("annotatedValues", Arrays.asList(enum1Node, enum2Node));

        Enum e = parser.parseEnum(enumsNode);

        Assert.assertEquals(enumName, e.getName());

        Set<EnumValue> values = e.getEnumValues();
        Assert.assertEquals(2, values.size());
        Assert.assertTrue(e.getEnumValues().contains(new EnumValue(e.getName(), enumValue1, enumDescription1)));
        Assert.assertTrue(e.getEnumValues().contains(new EnumValue(e.getName(), enumValue2, enumDescription2)));
    }

}
