package ca.bc.gov.nrs.vdyp.vri;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.assertEmpty;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.assertNext;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasSpecificEntry;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.vri.model.VriSite;

public class VriSiteParserTest {

	@Test
	public void testParseEmpty() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolver("test.dat", TestUtils.makeInputStream(/* empty */));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriSite>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		assertEmpty(stream);
	}

	@Test
	public void testParseOneSite() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolver(
				"test.dat",
				TestUtils.makeInputStream(
						"082F074/0071         2001 P 200 28.0 14.3        C CW 10.9          189.1 11",
						"082F074/0071         2001 Z   0  0.0  0.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VriSite>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var sites = assertNext(stream);

		assertThat(sites, iterableWithSize(1));
		assertThat(
				sites.iterator().next(), allOf(
						hasProperty("ageTotal", present(closeTo(200.0f))), //
						hasProperty("height", present(closeTo(28.0f))), //
						hasProperty("siteIndex", present(closeTo(14.3f))), //
						hasProperty("siteGenus", is("C")), //
						hasProperty("siteSpecies", is("CW")), //
						hasProperty("yearsToBreastHeight", present(closeTo(10.9f))), //
						hasProperty("breastHeightAge", present(closeTo(189.1f))), //
						hasProperty("siteCurveNumber", present(is(11)))
				)
		);

		assertEmpty(stream);
	}

	@Test
	public void testIgnoreIfNotPrimaryOrSecondary() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolver(
				"test.dat",
				TestUtils.makeInputStream(
						"082F074/0071         2001 X 100 28.0 14.3        C CW 10.9          189.1 11",
						"082F074/0071         2001 P 200 28.0 14.3        C CW 10.9          189.1 11",
						"082F074/0071         2001 Z   0  0.0  0.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VriSite>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var sites = assertNext(stream);

		assertThat(
				sites, containsInAnyOrder(
						allOf(
								hasProperty("ageTotal", present(closeTo(200.0f))), //
								hasProperty("height", present(closeTo(28.0f))), //
								hasProperty("siteIndex", present(closeTo(14.3f))), //
								hasProperty("siteGenus", is("C")), //
								hasProperty("siteSpecies", is("CW")), //
								hasProperty("yearsToBreastHeight", present(closeTo(10.9f))), //
								hasProperty("breastHeightAge", present(closeTo(189.1f))), //
								hasProperty("siteCurveNumber", present(is(11)))

						)
				)
		);

		assertEmpty(stream);
	}

	@Test
	public void testParseTwoSites() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolver(
				"test.dat",
				TestUtils.makeInputStream(
						"082F074/0071         2001 P 200 28.0 14.3        C CW 10.9          189.1 11",
						"082F074/0071         2001 P 200 32.0 14.6        H HW  9.7          190.3 37",
						"082F074/0071         2001 Z   0  0.0  0.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VriSite>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var sites = assertNext(stream);

		assertThat(
				sites, containsInAnyOrder(
						allOf(
								hasProperty("ageTotal", present(closeTo(200.0f))), //
								hasProperty("height", present(closeTo(28.0f))), //
								hasProperty("siteIndex", present(closeTo(14.3f))), //
								hasProperty("siteGenus", is("C")), //
								hasProperty("siteSpecies", is("CW")), //
								hasProperty("yearsToBreastHeight", present(closeTo(10.9f))), //
								hasProperty("breastHeightAge", present(closeTo(189.1f))), //
								hasProperty("siteCurveNumber", present(is(11)))
						),
						allOf(
								hasProperty("ageTotal", present(closeTo(200.0f))), //
								hasProperty("height", present(closeTo(32.0f))), //
								hasProperty("siteIndex", present(closeTo(14.6f))), //
								hasProperty("siteGenus", is("H")), //
								hasProperty("siteSpecies", is("HW")), //
								hasProperty("yearsToBreastHeight", present(closeTo(9.7f))), //
								hasProperty("breastHeightAge", present(closeTo(190.3f))), //
								hasProperty("siteCurveNumber", present(is(37)))
						)
				)
		);

		assertEmpty(stream);

	}

	@Test
	public void testParseTwoLayers() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolver(
				"test.dat",
				TestUtils.makeInputStream(
						"082F074/0071         2001 P 200 28.0 14.3        C CW 10.9          189.1 11",
						"082F074/0071         2001 S 200 32.0 14.6        H HW  9.7          190.3 37",
						"082F074/0071         2001 Z   0  0.0  0.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VriSite>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var sites = assertNext(stream);

		assertThat(
				sites, containsInAnyOrder(
						allOf(
								hasProperty("layer", is(LayerType.PRIMARY)), //
								hasProperty("ageTotal", present(closeTo(200.0f))), //
								hasProperty("height", present(closeTo(28.0f))), //
								hasProperty("siteIndex", present(closeTo(14.3f))), //
								hasProperty("siteGenus", is("C")), //
								hasProperty("siteSpecies", is("CW")), //
								hasProperty("yearsToBreastHeight", present(closeTo(10.9f))), //
								hasProperty("breastHeightAge", present(closeTo(189.1f))), //
								hasProperty("siteCurveNumber", present(is(11)))
						),
						allOf(
								hasProperty("layer", is(LayerType.SECONDARY)), //
								hasProperty("ageTotal", present(closeTo(200.0f))), //
								hasProperty("height", present(closeTo(32.0f))), //
								hasProperty("siteIndex", present(closeTo(14.6f))), //
								hasProperty("siteGenus", is("H")), //
								hasProperty("siteSpecies", is("HW")), //
								hasProperty("yearsToBreastHeight", present(closeTo(9.7f))), //
								hasProperty("breastHeightAge", present(closeTo(190.3f))), //
								hasProperty("siteCurveNumber", present(is(37)))
						)
				)
		);

		assertEmpty(stream);
	}

	@Test
	public void testParseTwoPolygons() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolver(
				"test.dat",
				TestUtils.makeInputStream(
						"082F074/0072         2002 P 200 28.0 14.3        C CW 10.9          189.1 11",
						"082F074/0072         2002 Z   0  0.0  0.0",
						"082F074/0071         2001 P 200 32.0 14.6        H HW  9.7          190.3 37",
						"082F074/0071         2001 Z   0  0.0  0.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VriSite>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var sites = assertNext(stream);

		assertThat(sites, iterableWithSize(1));
		assertThat(
				sites.iterator().next(), allOf(
						hasProperty("polygonIdentifier", is("082F074/0072         2002")), //
						hasProperty("layer", is(LayerType.PRIMARY)), //
						hasProperty("ageTotal", present(closeTo(200.0f))), //
						hasProperty("height", present(closeTo(28.0f))), //
						hasProperty("siteIndex", present(closeTo(14.3f))), //
						hasProperty("siteGenus", is("C")), //
						hasProperty("siteSpecies", is("CW")), //
						hasProperty("yearsToBreastHeight", present(closeTo(10.9f))), //
						hasProperty("breastHeightAge", present(closeTo(189.1f))), //
						hasProperty("siteCurveNumber", present(is(11)))
				)
		);

		sites = assertNext(stream);

		assertThat(sites, iterableWithSize(1));
		assertThat(
				sites.iterator().next(), allOf(
						hasProperty("polygonIdentifier", is("082F074/0071         2001")), //
						hasProperty("layer", is(LayerType.PRIMARY)), //
						hasProperty("ageTotal", present(closeTo(200.0f))), //
						hasProperty("height", present(closeTo(32.0f))), //
						hasProperty("siteIndex", present(closeTo(14.6f))), //
						hasProperty("siteGenus", is("H")), //
						hasProperty("siteSpecies", is("HW")), //
						hasProperty("yearsToBreastHeight", present(closeTo(9.7f))), //
						hasProperty("breastHeightAge", present(closeTo(190.3f))), //
						hasProperty("siteCurveNumber", present(is(37)))
				)

		);

		assertEmpty(stream);
	}

	@Disabled("TODO Convert from Species to Site")
	@Test
	public void testParseMutipleSites() throws Exception {

		var parser = new VriSiteParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolver(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 1 B  100.0B1  75.0B2  10.0B3   8.0B4   7.0",
						"01002 S000001 00     1970 Z      0.0     0.0     0.0     0.0     0.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_YIELD_SPEC_DIST_INPUT.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VriSite>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var sites = assertNext(stream);

		assertThat(
				sites,
				containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", is("01002 S000001 00     1970")),
								hasProperty("layer", is(LayerType.PRIMARY)), hasProperty("genus", is("B")),
								hasProperty("percentGenus", is(100.0f)),
								hasProperty(
										"speciesPercent",
										allOf(
												aMapWithSize(4),
												allOf(
														hasSpecificEntry("B1", is(75.0f)),
														hasSpecificEntry("B2", is(10.0f)),
														hasSpecificEntry("B3", is(8.0f)),
														hasSpecificEntry("B4", is(7.0f))
												)
										)
								)
						)
				)
		);

		assertEmpty(stream);
	}

}
