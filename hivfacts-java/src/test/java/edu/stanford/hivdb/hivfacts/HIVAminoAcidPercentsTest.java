package edu.stanford.hivdb.hivfacts;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HIVAminoAcidPercentsTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void testGetInstanceSuccess() {
		HIVAminoAcidPercents allall01 = HIVAminoAcidPercents.getInstance("all", "all");
		HIVAminoAcidPercents allall02 = HIVAminoAcidPercents.getInstance("all", "all");
		assertEquals("same singleton instance", allall01, allall02);
	}

	@Test
	public void testGetInstanceFailCase1() {
		expectedEx.expect(ExceptionInInitializerError.class);
		expectedEx.expectMessage("Invalid resource name (aapcnt/rx-all_subtype-E.json)");
		HIVAminoAcidPercents.getInstance("all", "E");
	}

	@Test
	public void testGetInstanceFailCase2() {
		expectedEx.expect(ExceptionInInitializerError.class);
		expectedEx.expectMessage("Invalid resource name (aapcnt/rx-aaaaaaa_subtype-all.json)");
		HIVAminoAcidPercents.getInstance("aaaaaaa", "all");
	}

	@Test
	public void testGsonLoad() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		assertEquals((99 + 560 + 288) * 23, allall.aminoAcidPcnts.size());
		HIVAminoAcidPercent mutPR1A = allall.aminoAcidPcnts.get(0);
		assertEquals(Gene.valueOf("HIV1PR"), mutPR1A.getGene());
		assertEquals(1, (int) mutPR1A.position);
		assertEquals('A', (char) mutPR1A.aa);
	}

	@Test
	public void testGetall() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		List<HIVAminoAcidPercent> allAAPcnts = allall.get();
		assertEquals(allall.aminoAcidPcnts, allAAPcnts); // equal values
		assertFalse(allall.aminoAcidPcnts == allAAPcnts); // different references
		assertEquals((99 + 560 + 288) * 23, allAAPcnts.size());
	}

	@Test
	public void testGetByGene() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		List<HIVAminoAcidPercent> gRTAAPcnts = allall.get(Gene.valueOf("HIV1RT"));
		assertEquals(560 * 23, gRTAAPcnts.size());
		int i = 0;
		for (int p = 1; p <= 560; p ++) {
			for (char aa : "ACDEFGHIKLMNPQRSTVWY_-*".toCharArray()) {
				HIVAminoAcidPercent mut = gRTAAPcnts.get(i ++);
				assertEquals(Gene.valueOf("HIV1RT"), mut.getGene());
				assertEquals(p, (int) mut.position);
				assertEquals(aa, (char) mut.aa);
			}
		}
	}

	@Test
	public void testGetByGenePos() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		List<HIVAminoAcidPercent> gpIN263AAPcnts = allall.get(Gene.valueOf("HIV1IN"), 263);
		assertEquals(23, gpIN263AAPcnts.size());
		int i = 0;
		for (char aa : "ACDEFGHIKLMNPQRSTVWY_-*".toCharArray()) {
			HIVAminoAcidPercent mut = gpIN263AAPcnts.get(i ++);
			assertEquals(Gene.valueOf("HIV1IN"), mut.getGene());
			assertEquals(263, (int) mut.position);
			assertEquals(aa, (char) mut.aa);
		}
	}

	@Test
	public void testGetByMut() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		HIVAminoAcidPercent mutIN263R = allall.get(Gene.valueOf("HIV1IN"), 263, 'R');
		assertEquals(Gene.valueOf("HIV1IN"), mutIN263R.getGene());
		assertEquals(263, (int) mutIN263R.position);
		assertEquals('R', (char) mutIN263R.aa);
	}

	@Test
	public void testGetHighestPercentValue() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		double highestVal = allall.getHighestAAPercentValue(Gene.valueOf("HIV1PR"), 23, "AHI");
		double expectedHighestVal = .0;
		for (char aa : "AHI".toCharArray()) {
			double pcntVal = allall.get(Gene.valueOf("HIV1PR"), 23, aa).percent;
			expectedHighestVal = Math.max(expectedHighestVal, pcntVal);
		}
		assertEquals(expectedHighestVal, highestVal, 1e-18);

		// These are intended to fail for every version update.
		// You must manually check and correct these numbers.
		assertEquals(0.00193290, highestVal, 1e-8);
		assertEquals(0.08111958, allall.getHighestAAPercentValue(Gene.valueOf("HIV1RT"), 67, "N"), 1e-8);
		assertEquals(0.00822416, allall.getHighestAAPercentValue(Gene.valueOf("HIV1RT"), 69, "KS"), 1e-8);
		assertEquals(0.04712358, allall.getHighestAAPercentValue(Gene.valueOf("HIV1PR"), 82, "IA"), 1e-8);
		assertEquals(0.08111958, allall.getHighestAAPercentValue(Gene.valueOf("HIV1RT"), 67, "N*"), 1e-8);
		assertEquals(0.0, allall.getHighestAAPercentValue(Gene.valueOf("HIV1RT"), 67, "*"), 1e-8);
		assertEquals(0.0, allall.getHighestAAPercentValue(Gene.valueOf("HIV1IN"), 1, ""), 1e-8);
	}

	@Test
	public void testContainsUnusualAA() {
		HIVAminoAcidPercents allall = HIVAminoAcidPercents.getInstance("all", "all");
		assertTrue(allall.containsUnusualAA(Gene.valueOf("HIV1RT"), 5, "I*"));
		assertFalse(allall.containsUnusualAA(Gene.valueOf("HIV1RT"), 67, "N"));
		assertTrue(allall.containsUnusualAA(Gene.valueOf("HIV1PR"), 82, "VIAD"));
		assertFalse(allall.containsUnusualAA(Gene.valueOf("HIV1RT"), 69, "_"));
		assertTrue(allall.containsUnusualAA(Gene.valueOf("HIV1PR"), 67, "-"));
	}

}
