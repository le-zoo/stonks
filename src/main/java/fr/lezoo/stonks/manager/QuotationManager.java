package fr.lezoo.stonks.manager;

import fr.lezoo.stonks.api.quotation.Quotation;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class QuotationManager {
    private final Map<String, Quotation> map = new HashMap<>();

    public void reload() {
        // TODO
    }

    public Quotation get(String id) {
        return map.get(formatId(id));
    }

    public boolean has(String id) {
        return map.containsKey(formatId(id));
    }

    public void register(Quotation quotation) {
        Validate.isTrue(!map.containsKey(quotation.getId()), "There is already a quotation with ID '" + quotation.getId() + "'");

        map.put(quotation.getId(), quotation);
    }

    public Collection<Quotation> getQuotations() {
        return map.values();
    }

    private String formatId(String str) {
        return str.toLowerCase().replace(" ", "-").replace("_", "-");
    }
}
