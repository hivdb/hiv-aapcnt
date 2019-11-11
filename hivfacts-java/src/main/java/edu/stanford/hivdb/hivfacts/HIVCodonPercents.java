package edu.stanford.hivdb.hivfacts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * There are two public methods: getHighestMutPrevalence, unusualMutations
 *
 */
public class HIVCodonPercents {

	final static protected Gson gson = new Gson();
	final static protected Map<String, HIVCodonPercents> singletons = new HashMap<>();

	final protected List<HIVCodonPercent> codonPcnts;
	final private Map<GenePosition, Map<String, HIVCodonPercent>> codonPcntMap = new HashMap<>();

	/**
	 * Get an HIVCodonPercents instance
	 *
	 * @param treatment "naive" or "art"
	 * @param subtype "all", "A", "B", "C", "D", "F", "G", "CRF01_AE", "CRF02_AG"
	 */
	public static HIVCodonPercents getInstance(String treatment, String subtype) {
		String resourceName = String.format("codonpcnt/rx-%s_subtype-%s.json", treatment, subtype);
		if (!singletons.containsKey(resourceName)) {
			singletons.put(resourceName, new HIVCodonPercents(resourceName));
		}
		return singletons.get(resourceName);
	}


	/**
	 * HIVAminoAcidPercents initializer
	 *
	 * @param resourceName
	 */
	protected HIVCodonPercents(String resourceName) {

		try (
			InputStream stream = this
				.getClass().getClassLoader()
				.getResourceAsStream(resourceName);
		) {
			String raw = IOUtils.toString(stream, StandardCharsets.UTF_8);
			codonPcnts = gson.fromJson(raw, new TypeToken<List<HIVCodonPercent>>(){}.getType());
		} catch (IOException|NullPointerException e) {
			throw new ExceptionInInitializerError(
				String.format("Invalid resource name (%s)", resourceName)
			);
		}

		for (HIVCodonPercent cdPcnt : codonPcnts) {
			GenePosition gp = cdPcnt.getGenePosition();
			codonPcntMap.putIfAbsent(gp, new LinkedHashMap<>());
			codonPcntMap.get(gp).put(cdPcnt.codon, cdPcnt);
		}
	}

	public List<HIVCodonPercent> get() {
		// make a copy in case of any modification
		return new ArrayList<>(codonPcnts);
	}

	public List<HIVCodonPercent> get(Gene gene) {
		return (codonPcnts
				.stream().filter(cdp -> cdp.getGene().equals(gene))
				.collect(Collectors.toList()));
	}

	public List<HIVCodonPercent> get(Gene gene, int pos) {
		return new ArrayList<>(
			codonPcntMap.getOrDefault(new GenePosition(gene, pos), Collections.emptyMap())
			.values());
	}

	public HIVCodonPercent get(Gene gene, int pos, String codon) {
		Map<String, HIVCodonPercent> posCodons =
			codonPcntMap.getOrDefault(new GenePosition(gene, pos), Collections.emptyMap());
		if (posCodons.containsKey(codon)) {
			return posCodons.get(codon);
		}
		else if (posCodons.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("Argument 'pos' is out of range: %d", pos));
		}
		else if (codon.matches("^(ins|del)$")) {
			int total = posCodons.values().iterator().next().total;
			char aa = codon.equals("ins") ? '_' : '-';
			HIVCodonPercent posCodon = new HIVCodonPercent(gene.getName(), pos, codon, aa, .0, 0, total);
			posCodons.put(codon, posCodon);
			return posCodon;
		}
		else if (codon.matches("^[ACGT]{3}$")) {
			int total = posCodons.values().iterator().next().total;
			HIVCodonPercent posCodon = new HIVCodonPercent(gene.getName(), pos, codon, 'X', .0, 0, total);
			posCodons.put(codon, posCodon);
			return posCodon;
		}
		else {
			throw new IllegalArgumentException(
				String.format("Invalid argument codon \"%s\" at %s%d", codon, gene, pos));
		}
	}

	/**
	 * Returns the highest codon prevalence associated with each of
	 * the codon in a mixture.
	 *
	 * @param gene
	 * @param pos
	 * @param codonMixture
	 *
	 * @return Double highest amino acid prevalence
	 */
	public Double getHighestCodonPercentValue(
		Gene gene, int pos, String... codonMixture
	) {
		Double pcntVal = 0.0;

		for (String cd : codonMixture) {
			double cdPcntVal = get(gene, pos, cd).percent;
			pcntVal = Math.max(pcntVal, cdPcntVal);
		}
		return pcntVal;
	}

}
