package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.SP0Definition;

/**
 * Parser for a SP0 Definition data file
 * 
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class SP0DefinitionParser implements ResourceParser<List<SP0Definition>>{
	
	private int num_sp0;
	
	LineParser lineParser = new LineParser()
		.strippedString(2, "alias")
		.space(1)
		.strippedString(32, "name")
		.space(1)
		.value(2, "preference", s->
			ValueParser.optional(ValueParser.INTEGER).parse(s)
				.flatMap(v->v==0 ? Optional.empty() : Optional.of(v)));
		;
	
	public SP0DefinitionParser() {
		super();
		this.num_sp0 = 16;
	}
	
	public SP0DefinitionParser(int num_sp0) {
		super();
		this.num_sp0 = num_sp0;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SP0Definition> parse(InputStream is) throws IOException, ResourceParseException {
		SP0Definition[] result = new SP0Definition[num_sp0];
		result = lineParser.parse(is, result, (v, r)->{
			String alias = (String) v.get("alias");
			Optional<Integer> preference = (Optional<Integer>) v.get("preference");
			String name = (String) v.get("name");
			Integer lineNumber = (Integer) v.get(LineParser.LINE_NUMBER_KEY);
			
			var defn = new SP0Definition(alias, preference, name);
			int p = preference.orElse(lineNumber);
			
			if(p>num_sp0) {
				throw new ValueParseException(Integer.toString(p),String.format("Preference %d is larger than %d", p, num_sp0));
			}
			if(p<1) {
				throw new ValueParseException(Integer.toString(p),String.format("Preference %d is less than %d", p, 0));
			}
			if(r[p-1]!=null) {
				throw new ValueParseException(Integer.toString(p),String.format("Preference %d has already been set to %s", p, r[p-1].getAlias()));
			}
			
			r[p-1] = defn;
			return r;
		});
		
		return Collections.unmodifiableList(Arrays.asList(result));
	}
	
}
