/**
 * Created by huimin on 10/1/15.
 */
public enum RelationSymbol {

    extension("--|>"),
    association("--"),
    //composition("*--"),
    //aggregation("o--"),
    dependency("..>"),
    implement("..|>");



    private String value;

    RelationSymbol(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

}
