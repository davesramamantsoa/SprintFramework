package framework.mg.itu.view;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String view;
    private final Map<String, Object> data = new HashMap<>();

    public ModelAndView() {
    }

    public ModelAndView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setAttribut(String key, Object value) {
        this.data.put(key, value);
    }
}