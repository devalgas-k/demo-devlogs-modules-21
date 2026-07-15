package com.example.listdemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListBehaviorTest {

    @Test
    @DisplayName("Arrays.asList() allows set() but not add()")
    void arraysAsList_setWorks_addFails() {
        String[] array = {"A", "B", "C"};
        List<String> list = Arrays.asList(array);

        // set() works
        list.set(0, "Z");
        assertEquals("Z", list.get(0));
        assertEquals("Z", array[0]);  // Also modifies original

        // add() fails
        assertThrows(UnsupportedOperationException.class, () -> list.add("D"));

        // remove() fails
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    }

    @Test
    @DisplayName("List.of() does not allow set(), add(), or remove()")
    void listOf_allModificationsThrow() {
        List<String> list = List.of("A", "B", "C");

        // set() fails
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, "Z"));

        // add() fails
        assertThrows(UnsupportedOperationException.class, () -> list.add("D"));

        // remove() fails
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    }

    @Test
    @DisplayName("List.of() rejects null elements")
    void listOf_rejectsNull() {
        assertThrows(NullPointerException.class, () -> List.of("A", null, "C"));
    }

    @Test
    @DisplayName("Arrays.asList() accepts null elements")
    void arraysAsList_acceptsNull() {
        List<String> list = Arrays.asList("A", null, "C");
        assertNull(list.get(1));
    }

    @Test
    @DisplayName("Arrays.asList() creates view — modifications affect original")
    void arraysAsList_isView() {
        String[] array = {"A", "B", "C"};
        List<String> list = Arrays.asList(array);

        list.set(0, "Z");
        assertEquals("Z", array[0]);

        array[1] = "Y";
        assertEquals("Y", list.get(1));
    }

    @Test
    @DisplayName("List.of() creates independent copy")
    void listOf_isIndependentCopy() {
        String[] array = {"A", "B", "C"};
        List<String> list = List.of(array);

        array[0] = "Z";
        assertEquals("A", list.get(0));  // Unchanged

        assertThrows(UnsupportedOperationException.class, () -> list.set(0, "Z"));
    }

    @Test
    @DisplayName("Arrays.asList() with primitive array creates wrong list")
    void arraysAsList_primitiveArray_pitfall() {
        int[] primitives = {1, 2, 3};

        // WRONG: Creates List<int[]> with ONE element
        List<int[]> list = Arrays.asList(primitives);
        assertEquals(1, list.size());
        assertEquals(primitives, list.get(0));
    }

    @Test
    @DisplayName("Correct way to convert primitive array to List")
    void primitiveArray_toList_correct() {
        int[] primitives = {1, 2, 3};

        // CORRECT: Box primitives
        List<Integer> list = Arrays.stream(primitives).boxed().toList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("List.copyOf() creates immutable copy")
    void listCopyOf_createsImmutableCopy() {
        List<String> original = new java.util.ArrayList<>(List.of("A", "B"));
        List<String> copy = List.copyOf(original);

        assertThrows(UnsupportedOperationException.class, () -> copy.add("C"));
        assertThrows(UnsupportedOperationException.class, () -> copy.set(0, "Z"));
    }

    @Test
    @DisplayName("Collections.unmodifiableList() prevents modifications")
    void unmodifiableList_preventsModifications() {
        List<String> mutable = new java.util.ArrayList<>(List.of("A", "B"));
        List<String> unmodifiable = Collections.unmodifiableList(mutable);

        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("C"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.set(0, "Z"));
    }

    @Test
    @DisplayName("All read operations work on List.of()")
    void listOf_readOperationsWork() {
        List<String> list = List.of("A", "B", "C");

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
        assertTrue(list.contains("B"));
        assertFalse(list.isEmpty());
        assertEquals(0, list.indexOf("A"));
        assertEquals(-1, list.indexOf("Z"));
    }
}
