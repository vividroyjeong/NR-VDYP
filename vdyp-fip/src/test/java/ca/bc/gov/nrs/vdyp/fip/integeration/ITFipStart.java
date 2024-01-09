package ca.bc.gov.nrs.vdyp.fip.integeration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

import ca.bc.gov.nrs.vdyp.fip.*;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.*;
import ca.bc.gov.nrs.vdyp.io.write.ControlFileWriter;
import ca.bc.gov.nrs.vdyp.math.FloatMath;

class ITFipStart {

	@TempDir
	Path configDir;

	@TempDir
	Path inputDir;

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	Path outputDir;

	private Path baseControlFile;
	private Path ioControlFile;

	static Path copyResource(Class<?> klazz, String path, Path destination) throws IOException {
		Path source = testResourcePath(klazz, path);
		Path result = destination.resolve(path);
		Files.copy(source, result);
		return result;

	}

	private static Path testResourcePath(Class<?> klazz, String path) {
		try {
			var resourceUri = klazz.getResource(path);
			assertThat("Could not find resource " + path, resourceUri, notNullValue());
			Path source = Paths.get(resourceUri.toURI());
			return source;
		} catch (URISyntaxException e) {
			Assumptions.abort(e.getMessage());
			return null;
		}

	}

	private static final String[] COE_FILES = new String[] { "Becdef.dat", "SP0DEF_v0.dat", "VGRPDEF1.DAT", "DGRP.DAT",
			"BGRP.DAT", "SIEQN.PRM", "SIAGEMAX.PRM", "GRPBA1.DAT", "GMODBA1.DAT", "FIPSTKR.PRM", "REGBA25.coe",
			"REGDQ26.coe", "UPPERB02.COE", "REGYHLP.COE", "REGYHLPA.COE", "REGYHLPB.DAT", "REGHL.COE", "REGDQI04.COE",
			"COMPLIM.COE", "REGBAC.DAT", "REGDQC.DAT", "REGPR1C.DAT", "REGBA2C.DAT", "REGDQ4C.DAT", "REGHL1C.DAT",
			"REGV1C.DAT", "VTOTREG4.COE", "REGVU.COE", "REGVCU.COE", "REGVDU.COE", "REGVWU.COE", "REGBREAK.COE",
			"VETVOL1.DAT", "VETDQ2.DAT", "REGBAV01.COE", "mod19813.prm" };

	private static final String[] INPUT_FILES = new String[] { "fip_l1.dat", "fip_ls1.dat", "fip_p1.dat" };

	private static final String POLYGON_OUTPUT_NAME = "vri_poly.dat";
	private static final String SPECIES_OUTPUT_NAME = "vri_spec.dat";
	private static final String UTILIZATION_OUTPUT_NAME = "vri_util.dat";

	@BeforeEach
	void init() throws IOException {
		baseControlFile = copyResource(ControlFileParserTest.class, "FIPSTART.CTR", configDir);
		Files.createDirectory(configDir.resolve("coe"));
		for (String filename : COE_FILES) {
			copyResource(ControlFileParserTest.class, "coe/" + filename, configDir);
		}
		for (String filename : INPUT_FILES) {
			copyResource(FipControlParserTest.class, filename, inputDir);
		}

		// Create a second control file pointing to the input and output
		ioControlFile = inputDir.resolve("fip.ctr");
		try (
				var os = Files.newOutputStream(ioControlFile); //
				var writer = new ControlFileWriter(os);
		) {
			writer.writeComment("Generated supplementarty control file for integration testing");
			writer.writeBlank();
			writer.writeComment("Inputs");
			writer.writeBlank();
			writer.writeEntry(11, inputDir.resolve("fip_p1.dat").toString(), "FIP Polygon Input");
			writer.writeEntry(12, inputDir.resolve("fip_l1.dat").toString(), "FIP Layer Input");
			writer.writeEntry(13, inputDir.resolve("fip_ls1.dat").toString(), "FIP Species Input");
			writer.writeBlank();
			writer.writeComment("Outputs");
			writer.writeBlank();
			writer.writeEntry(15, outputDir.resolve(POLYGON_OUTPUT_NAME).toString(), "VRI Polygon Output");
			writer.writeEntry(16, outputDir.resolve(SPECIES_OUTPUT_NAME).toString(), "VRI Species Output");
			writer.writeEntry(18, outputDir.resolve(UTILIZATION_OUTPUT_NAME).toString(), "VRI Utilization Output");

		}
	}

	@Test
	void noControlFile() throws IOException, ResourceParseException, ProcessingException {
		try (var app = new FipStart();) {

			var resolver = new FileSystemFileResolver(configDir);

			Assertions.assertThrows(IllegalArgumentException.class, () -> app.init(resolver));
		}
	}

	@Test
	void controlFileDoesntExist() throws IOException, ResourceParseException, ProcessingException {
		try (var app = new FipStart();) {

			var resolver = new FileSystemFileResolver(configDir);

			Assertions.assertThrows(NoSuchFileException.class, () -> app.init(resolver, "FAKE"));
		}
	}

	public void assertFileExists(Path path) {
		assertTrue(Files.exists(path), path + " does not exist");
	}

	public void assertFileMatches(Path path, Path expected, BiPredicate<String, String> compare) throws IOException {

		try (
				var testStream = Files.newBufferedReader(path); //
				var expectedStream = Files.newBufferedReader(expected);
		) {
			for (int i = 1; true; i++) {
				String testLine = testStream.readLine();
				String expectedLine = expectedStream.readLine();

				if (testLine == null && expectedLine == null) {
					return;
				}
				if (testLine == null) {
					fail(
							"File " + path + " did not match " + expected
									+ ". Missing expected lines. The first missing line (" + i + ") was:\n"
									+ expectedLine
					);
				}
				if (expectedLine == null) {
					fail(
							"File " + path + " did not match " + expected
									+ ". Unexpected lines at the end. The first unexpected line (" + i + ") was:\n"
									+ testLine
					);
				}

				if (!compare.test(testLine, expectedLine)) {
					fail(
							"File " + path + " did not match " + expected + ". The first line (" + i
									+ ") to not match was: \n [Expected]: " + expectedLine + "\n   [Actual]: "
									+ testLine
					);
				}

			}
		}
	}

	static final Pattern UTIL_LINE_MATCHER = Pattern
			.compile("^(.{27})(?:(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{6}))?$", Pattern.MULTILINE);

	BiPredicate<String, String> floatStringsWithin(float relativeThreshold, float absoluteThreshold) {

		return new BiPredicate<>() {

			@Override
			public boolean test(String actual, String expected) {
				if (actual == null && expected == null) {
					return true;
				}

				if (actual == null || expected == null) {
					return false;
				}

				float actualValue = Float.parseFloat(actual);
				float expectedValue = Float.parseFloat(expected);

				float threshold = Math.max(expectedValue * relativeThreshold, absoluteThreshold);

				return FloatMath.abs(actualValue - expectedValue) < threshold;
			}

		};

	}

	BiPredicate<String, String> floatStringsWithin() {
		return floatStringsWithin(0.01f, 0.0001f);
	}

	boolean linesMatch(String actual, String expected) {
		var actualMatch = UTIL_LINE_MATCHER.matcher(actual);
		var expectedMatch = UTIL_LINE_MATCHER.matcher(expected);
		if (!actualMatch.find()) {
			return false;
		}
		if (!expectedMatch.find()) {
			return false;
		}

		List<BiPredicate<String, String>> checks = List.of(
				String::equals, Objects::equals, //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin() //
		);

		if (actualMatch.groupCount() != expectedMatch.groupCount()) {
			return false;
		}
		for (int i = 0; i < expectedMatch.groupCount(); i++) {
			if (!checks.get(i).test(actualMatch.group(i + 1), expectedMatch.group(i + 1))) {
				return false;
			}
		}
		return true;
	}

	@Test
	void controlFile() throws IOException, ResourceParseException, ProcessingException {
		try (var app = new FipStart();) {

			var resolver = new FileSystemFileResolver(configDir);

			app.init(resolver, baseControlFile.toString(), ioControlFile.toString());

			app.process();
		}

		assertFileExists(outputDir.resolve(POLYGON_OUTPUT_NAME));
		assertFileExists(outputDir.resolve(SPECIES_OUTPUT_NAME));
		assertFileExists(outputDir.resolve(UTILIZATION_OUTPUT_NAME));

		assertFileMatches(
				outputDir.resolve(POLYGON_OUTPUT_NAME), testResourcePath(FipControlParserTest.class, "vp_1.dat"),
				String::equals
		);
		assertFileMatches(
				outputDir.resolve(SPECIES_OUTPUT_NAME), testResourcePath(FipControlParserTest.class, "vs_1.dat"),
				String::equals
		);
		assertFileMatches(
				outputDir.resolve(UTILIZATION_OUTPUT_NAME), testResourcePath(FipControlParserTest.class, "vu_1.dat"),
				this::linesMatch
		);

	}

	@Test
	void utilizationFileLineMatcherSelfTest() {
		assertTrue(
				linesMatch(
						"01004 S000037 00     1957 P 12 PL  1  0.00775     0.98  -9.0000   0.0363   0.0079   0.0074   0.0073   0.0071  10.0",
						"01004 S000037 00     1957 P 12 PL  1  0.00774     0.98  -9.0000   0.0363   0.0079   0.0073   0.0073   0.0071  10.0"
				)
		);

	}
}
