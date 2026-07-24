package nodes;

import axiom.AxiomContext;
import gen.Messages.RawOfxInput;
import gen.Messages.ValidateStructureResult;
import java.util.Map;

public class ValidateStructure {

    /**
     * Validate an OFX/QFX document's basic structural correctness — a
     * parseable root &lt;OFX&gt; element, a sign-on response, and at least
     * one statement — and report the specific issues found. Never throws
     * on a malformed document: a document that fails to parse at all is
     * reported as valid=false with the parse failure listed as an issue,
     * not as a top-level error (the top-level error field is reserved for
     * input the node could not even attempt to validate: empty or a
     * rejected DOCTYPE).
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The raw OFX/QFX document text.
     */
    public static ValidateStructureResult validateStructure(AxiomContext ax, RawOfxInput input) {
        ax.log().info("validateStructure handling", Map.of());
        return OfxSupport.validateStructure(input.getOfxText());
    }
}
