package com.mutation;

public abstract class StringInjector extends StringMutationOperator{


    public StringInjector() {

    }

    protected Integer locate(String target) {
        Integer lineNumber = 0;
        for (String line: this.units) {
            if (line.contains(target)) {
                return lineNumber;
            }
            lineNumber++;
        }
        return -1;
    }

    protected Integer locateLast(String target) {
        int lineNumber = -1;
        int counter = 0;
        for (String line: this.units) {
            if (line.contains(target)) {
                lineNumber = counter;
            }
            counter++;
        }
        return lineNumber;
    }


    protected void inject(Integer index, String toBeInserted) {
        this.units.add(index, toBeInserted);
    }


}
