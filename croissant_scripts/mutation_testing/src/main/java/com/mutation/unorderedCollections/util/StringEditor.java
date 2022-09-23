package com.mutation.unorderedCollections.util;
import java.util.*;

public class StringEditor {
    String start;
    String end;
    String delimiter;

    public StringEditor(String start, String end, String delimiter) {
        this.start = start;
        this.end = end;
        this.delimiter = delimiter;
    }


    public String createStringRepresentation(List<String> contents) {
        StringBuilder mutatedValueBuilder = new StringBuilder();
        StringJoiner mutatedValueJoiner = new StringJoiner(this.delimiter);

        for (CharSequence element: contents) {
            mutatedValueJoiner.add(element);
        }

        mutatedValueBuilder.setLength(0);
        mutatedValueBuilder.append(this.start);
        mutatedValueBuilder.append(mutatedValueJoiner);
        mutatedValueBuilder.append(this.end);

        replaceAll(mutatedValueBuilder,"(\")", "");

        return mutatedValueBuilder.toString();
    }

    public List<String> getContents(String stringRepresentation) {

        StringBuilder stringBuilder = new StringBuilder(stringRepresentation);

        replaceAll(stringBuilder,this.start, "");
        replaceAll(stringBuilder,this.end, "");
        replaceAll(stringBuilder,"(\")", "");

        return Arrays.asList(stringBuilder.toString().split(this.delimiter));
    }

    /**
     * replaces all occurences of a character in a string builder
     * @param builder String builder which contains the characters of the string that is being edited
     * @param original What is intended to be changed
     * @param changed What will original be replaced with
     */
    public static void replaceAll(StringBuilder builder, String original, String changed) {
        int index = builder.indexOf(original);
        while (index != -1) {
            builder.replace(index, index + original.length(), changed);
            index += changed.length();
            index = builder.indexOf(original, index);
        }
    }

    /**
     * Shuffles inputted split string representation of the unordered collection.
     * Makes sure the shuffled version is distinct from the original version
     * @param elements elements of the string representation such as ["x", "y", "z"] for "[x,y,z]"
     * @throws Exception throws an exception if it cannot shuffle elements (all elements are the same)
     */
    public void shuffle(List<String> elements) {
        if (elements.stream().distinct().count() <= 1) {
            return;
        }
        List<String> copy = new ArrayList<>(elements);

        do {
            Collections.shuffle(elements);
        } while (elements.equals(copy));
    }

}
