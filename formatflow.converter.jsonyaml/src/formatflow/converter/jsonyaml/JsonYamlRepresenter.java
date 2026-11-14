
package formatflow.converter.jsonyaml;

import java.math.BigDecimal;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class JsonYamlRepresenter extends Representer {

    public JsonYamlRepresenter(DumperOptions options) {
        super(options);

        this.representers.put(BigDecimal.class, data -> {
            BigDecimal number = (BigDecimal) data;

            boolean isInteger =
                    number.stripTrailingZeros().scale() <= 0;

            return representScalar(
                    isInteger ? Tag.INT : Tag.FLOAT,
                    number.toPlainString()
            );
        });
    }
}
