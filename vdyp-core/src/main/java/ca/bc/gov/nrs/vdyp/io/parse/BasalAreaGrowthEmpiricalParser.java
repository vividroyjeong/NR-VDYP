package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;

public class BasalAreaGrowthEmpiricalParser implements ControlMapSubResourceParser<MatrixMap2<String, String, Coefficients>> {

	public static final String CONTROL_KEY = "BA_GROWTH_EMPIRICAL";

	public static final String BEC_ZONE_ID_KEY = "BecId";
	public static final String INDEX_KEY = "index";
	public static final String INDICATOR_KEY = "indicator";
	public static final String COEFFICIENTS_KEY = "coefficients";

	private int NUM_COEFFICIENTS = 8;
	private int NUM_SPECIES = 16;

	public BasalAreaGrowthEmpiricalParser() {
		this.lineParser = new LineParser() {

			@Override
			public boolean isIgnoredLine(String line) {
				return line.substring(0, Math.min(4,  line.length())).trim().length() == 0;
			}

		}
		.value(4, BEC_ZONE_ID_KEY, ValueParser.STRING)
		.space(2)
		.value(1, INDEX_KEY, ValueParser.INTEGER)
		.value(2, INDICATOR_KEY, ValueParser.INTEGER)
		.multiValue(NUM_SPECIES, 8, COEFFICIENTS_KEY, ValueParser.FLOAT);
	}

	LineParser lineParser;

	@Override
	public MatrixMap2<String, String, Coefficients> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		var becAliases = BecDefinitionParser.getBecs(control).getBecAliases();
		var sp0Aliases = GenusDefinitionParser.getSpeciesAliases(control);
		MatrixMap2<String, String, Coefficients> result = new MatrixMap2Impl<>(
				becAliases, sp0Aliases, (k1, k2) -> Coefficients.empty(NUM_COEFFICIENTS, 0)
		);
		
		lineParser.parse(is, result, (v, r) -> {
			var becZoneId = (String)v.get(BEC_ZONE_ID_KEY);
			var index = (int) v.get(INDEX_KEY);
			var indicator = (int) v.get(INDICATOR_KEY);
			
			@SuppressWarnings("unchecked")
			var specCoefficients = (List<Float>) v.get(COEFFICIENTS_KEY);

			if (index < 0 || index >= NUM_COEFFICIENTS) {
				throw new ValueParseException("Index value " + index + " is out of range; expecting a value from 0 to 7");
			}
			
			if (indicator < 0 || indicator > 1) {
				throw new ValueParseException("Indicator value " + indicator + " is out of range; expecting either 0 or 1");
			}
			
			BecDefinitionParser.getBecs(control).get(becZoneId).orElseThrow(() -> new ValueParseException("BEC Zone Id " + becZoneId + " is not a recognized BEC Zone"));
			
			var specIt = sp0Aliases.iterator();
			
			int coefficientIndex = 0;
			while (specIt.hasNext()) {
				
				Coefficients coefficients = result.get(becZoneId, specIt.next());
				Float coe = specCoefficients.get(coefficientIndex);
				coefficients.setCoe(index, coe);
				
				if (indicator == 0)
					break;
				
				coefficientIndex += 1;
			}

			return r;
		}, control);

		return result;
	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}
}
