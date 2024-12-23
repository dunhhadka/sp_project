package org.example.order.async;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestJSON {

    class Student {
        private String id, name;

        public Student(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void test() {
        toJSON(new Student("123", "ABC"));
    }

    public static String toJSON(Object object) {
        var methods = object.getClass().getMethods();
        for (var method : methods) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {

            }
        }

        return "";
    }

}
