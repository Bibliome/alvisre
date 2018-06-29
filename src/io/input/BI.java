package io.input;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.ParseException;

import corpus.Corpus;
import corpus.NEtype;
import corpus.RelationType;
import corpus.Schema;
import exceptions.UnexpectedDatasetFormat;

public class BI extends BioNLP11 {
	public BI(String path) throws ParseException {
		super(path);
		this.type = "BI";
		this.schema = createSchema();
	}
	
	
	public void importFiles(Double verbose) throws NumberFormatException, IOException {
		this.verbose = verbose;
		this.corpus = new Corpus();
		File folder = new File( inputDir);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null) throw new UnexpectedDatasetFormat("Error G39: The path is probably wrong, or there are no files");
		if (listOfFiles.length == 0)
			throw new UnexpectedDatasetFormat("Where are the files, mate?");
		for (File file : listOfFiles) {
			String filename = file.getName();
			if(verbose > 1) System.out.println("Reading " + filename);
			if (filename.startsWith("PMID")) {
				String[] nameparts = filename.split("\\.");
				if (nameparts.length < 2) {
					if (verbose > 1) System.out.println(filename);
				}
				String id = nameparts[0];
				String suffix = nameparts[1];
				if (suffix.equals("txt")) {
					importSingleFile(this.corpus, file, id);
				}
			}
		}
	}

	public static Schema createSchema() {
		Schema schema = new Schema();
		NEtype GeneFamily = new NEtype("GeneFamily");
		NEtype Protein = new NEtype("Protein");
		NEtype ProteinFamily = new NEtype("ProteinFamily");
		NEtype PolymeraseComplex = new NEtype("Polymerase");
		NEtype GeneProduct = new NEtype("GeneProduct");
		NEtype Site = new NEtype("Site");
		NEtype Promoter = new NEtype("Promoter");
		NEtype Gene = new NEtype("Gene");
		NEtype GeneComplex = new NEtype("GeneComplex");
		NEtype Regulon = new NEtype("Regulon");
		NEtype Transcription = new NEtype("Transcription");
		NEtype Expression = new NEtype("Expression");
		NEtype Action = new NEtype("Action");
		List<NEtype> netypes = Arrays.asList(Regulon, Gene, GeneFamily, GeneComplex, Protein, ProteinFamily, PolymeraseComplex, GeneProduct,
				Promoter, Site, Action, Expression, Transcription);
		List<RelationType> relationtypes = new ArrayList<RelationType>();
		/*
		 * RegulonDependence Regulon : [Regulon] Target : [GeneEntity |
		 * ProteinEntity]
		 */
		RelationType RegulonDependance = new RelationType("RegulonDependence", "Agent", Arrays.asList(Regulon), "Target", Arrays.asList(Gene,
				GeneFamily, GeneComplex, Protein, ProteinFamily, PolymeraseComplex, GeneProduct));
		relationtypes.add(RegulonDependance);
		/*
		 * BindTo Agent : [ ProteinEntity ] Target : [ Site | Promoter | Gene |
		 * GeneComplex ]
		 */
		RelationType BindTo = new RelationType("BindTo", "Agent", Arrays.asList(Protein, ProteinFamily, PolymeraseComplex, GeneProduct), "Target",
				Arrays.asList(Site, Promoter, Gene, GeneComplex));
		relationtypes.add(BindTo);
		/*
		 * TranscriptionFrom Transcription : [ Transcription | Expression ] Site
		 * : [ Site | Promoter ]
		 */
		RelationType TranscriptionFrom = new RelationType("TranscriptionFrom", "Process", Arrays.asList(Transcription, Expression), "Promoter",
				Arrays.asList(Site, Promoter));
		relationtypes.add(TranscriptionFrom);
		/*
		 * RegulonMember Regulon : [ Regulon ] Member : [ GeneEntity |
		 * ProteinEntity ]
		 */
		RelationType RegulonMember = new RelationType("RegulonMember", "Regulon", Arrays.asList(Regulon), "Member", Arrays.asList(Gene, GeneFamily,
				GeneComplex, Protein, ProteinFamily, PolymeraseComplex, GeneProduct));
		relationtypes.add(RegulonMember);
		/*
		 * SiteOf Site : [ Site ] Entity : [ Site | Promoter | GeneEntity ]
		 */
		RelationType SiteOf = new RelationType("SiteOf", "Site", Arrays.asList(Site), "Entity", Arrays.asList(Site, Promoter, Gene, GeneFamily,
				GeneComplex));
		relationtypes.add(SiteOf);
		/*
		 * TranscriptionBy Transcription : [ Transcription ] Agent : [
		 * ProteinEntity ]
		 */
		RelationType TranscriptionBy = new RelationType("TranscriptionBy", "Transcription", Arrays.asList(Transcription), "Polymerase", Arrays.asList(
				Protein, ProteinFamily, PolymeraseComplex, GeneProduct));
		relationtypes.add(TranscriptionBy);
		/*
		 * PromoterOf Promoter : [ Promoter ] Gene : [ GeneEntity |
		 * ProteinEntity ]
		 */
		RelationType PromoterOf = new RelationType("PromoterOf", "Agent", Arrays.asList(Promoter), "Target", Arrays.asList(Gene, GeneFamily,
				GeneComplex, Protein, ProteinFamily, PolymeraseComplex, GeneProduct));
		relationtypes.add(PromoterOf);
		/*
		 * PromoterDependence Promoter : [ Promoter ] Protein : [ GeneEntity |
		 * ProteinEntity ]
		 */
		RelationType PromoterDependence = new RelationType("PromoterDependence", "Agent", Arrays.asList(Promoter), "Target", Arrays.asList(Gene,
				GeneFamily, GeneComplex, Protein, ProteinFamily, PolymeraseComplex, GeneProduct));
		relationtypes.add(PromoterDependence);
		/*
		 * ActionTarget Action : [ Action | Expression | Transcription ] Target
		 * : [ * ]
		 */
		RelationType ActionTarget = new RelationType("ActionTarget", "Action", Arrays.asList(Action, Expression, Transcription), "Target",
				Arrays.asList(Regulon, Gene, GeneFamily, GeneComplex, Protein, ProteinFamily, PolymeraseComplex, GeneProduct, Promoter, Site, 
						Expression));
		relationtypes.add(ActionTarget);
		/*
		 * Interaction Agent : [ GeneEntity | ProteinEntity ] Target : [
		 * GeneEntity | ProteinEntity ]
		 */
		RelationType Interaction = new RelationType("Interaction", "Agent", Arrays.asList(Gene, GeneFamily, GeneComplex, Protein, ProteinFamily,
				PolymeraseComplex, GeneProduct), "Target", Arrays.asList(Gene, GeneFamily, GeneComplex, Protein, ProteinFamily, PolymeraseComplex,
				GeneProduct));
		relationtypes.add(Interaction);

		schema.DefinedRelationTypes = relationtypes;
		schema.DefinedTypes = netypes;
//		schema.SyntacticRelationsBetweenTerms = false;
		return schema;
	}


	



}
