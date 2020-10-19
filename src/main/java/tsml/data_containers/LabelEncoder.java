package tsml.data_containers;

import utilities.Utilities;

import java.util.*;
import java.util.stream.Collectors;

public class LabelEncoder extends AbstractList<String> {
    private List<String> classes;
    private Map<String, Integer> indexMap;
    
    public LabelEncoder() {}
    
    public LabelEncoder(List<String> classes) {
        fit(classes);
    }
    
    public LabelEncoder(String[] classes) {
        this(Arrays.asList(classes));
    }
    
    public void fit(List<String> labels) {
        this.classes = labels.stream().distinct().collect(Collectors.toList());
        indexMap = Utilities.mapIndices(labels);
    }
    
    public List<Integer> fitTransform(List<String> labels) {
        fit(labels);
        return transform(labels);
    }
    
    public List<Integer> transform(List<String> labels) {
        return labels.stream().map(this::transform).collect(Collectors.toList());
    }
    
    public List<String> inverseTransform(List<Integer> indices) {
        return indices.stream().map(this::inverseTransform).collect(Collectors.toList());
    }
    
    public List<String> getClasses() {
        return classes;
    }
    
    public String inverseTransform(Integer index) {
        if(classes == null) throw new IllegalStateException("must fit first");
        if(index < 0 || index >= classes.size()) {
            throw new IllegalArgumentException("no mapping to label for " + index);
        }
        return classes.get(index);
    }
    
    public Integer transform(String label) {
        if(indexMap == null) throw new IllegalStateException("must fit first");
        Integer index = indexMap.get(label);
        if(index == null) {
            throw new IllegalArgumentException("no mapping to index for label " + label);
        }
        return index;
    }

    @Override public int size() {
        return classes.size();
    }

    @Override public String get(final int i) {
        return inverseTransform(i);
    }

    @Override public void add(final int i, final String s) {
        // move all mapping >i up one
        for(int j = i; j < classes.size(); j++) {
            String label = classes.get(j);
            indexMap.put(label, j + 1);
        }
        classes.add(i, s);
    }

    @Override public String remove(final int i) {
        String label = classes.remove(i);
        indexMap.remove(label);
        return label;
    }

    @Override public void clear() {
        indexMap = null;
        classes = null;
    }

    @Override public String set(final int i, final String s) {
        return classes.set(i, s);
    }

    @Override public boolean equals(final Object o) {
        if(o instanceof LabelEncoder) {
            LabelEncoder other = (LabelEncoder) o;
            return classes.equals(other.classes);
        }
        return false;
    }
}
