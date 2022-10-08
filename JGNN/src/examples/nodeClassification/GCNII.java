package nodeClassification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import mklab.JGNN.adhoc.IdConverter;
import mklab.JGNN.adhoc.ModelBuilder;
import mklab.JGNN.adhoc.parsers.GCNBuilder;
import mklab.JGNN.core.Matrix;
import mklab.JGNN.nn.Model;
import mklab.JGNN.nn.ModelTraining;
import mklab.JGNN.core.Slice;
import mklab.JGNN.core.Tensor;
import mklab.JGNN.core.matrix.SparseMatrix;
import mklab.JGNN.core.matrix.WrapRows;
import mklab.JGNN.core.tensor.SparseTensor;
import mklab.JGNN.nn.initializers.XavierNormal;
import mklab.JGNN.nn.loss.CategoricalCrossEntropy;
import mklab.JGNN.nn.optimizers.Adam;

public class GCNII {

	public static void main(String[] args) throws Exception {
		IdConverter nodes2Ids = new IdConverter();
		IdConverter class2Ids = new IdConverter();
		ArrayList<Tensor> rows = new ArrayList<Tensor>();
		ArrayList<Integer> classes = new ArrayList<Integer>();
		try(BufferedReader reader = new BufferedReader(new FileReader("downloads/citeseer/citeseer.feats"))){
			String line = reader.readLine();
			while (line != null) {
				String[] cols = line.split(",");
				if(cols.length<2)
					continue;
				nodes2Ids.getOrCreateId(cols[0]);
				Tensor features = new SparseTensor(cols.length-2);
				for(int col=0;col<cols.length-2;col++)
					features.put(col, Double.parseDouble(cols[col+1]));
				rows.add(features);
				classes.add((int)class2Ids.getOrCreateId(cols[cols.length-1]));
				line = reader.readLine();
			}
		}
		Matrix features = new WrapRows(rows).toSparse();
		rows.clear();
		Matrix labels = new SparseMatrix(features.getRows(), class2Ids.size());
		for(int row=0;row<classes.size();row++)
			labels.put(row, classes.get(row), 1);
	
		Matrix adjacency = new SparseMatrix(nodes2Ids.size(), nodes2Ids.size());
		try(BufferedReader reader = new BufferedReader(new FileReader("downloads/citeseer/citeseer.graph"))){
			String line = reader.readLine();
			while (line != null) {
				String[] cols = line.split(",");
				if(cols.length<2)
					continue;
				long from = nodes2Ids.getId(cols[0]);
				long to = nodes2Ids.getId(cols[1]);
				adjacency.put(from, to, 1).put(to, from, 1);
				line = reader.readLine();
			}
		}
		adjacency.setMainDiagonal(1).setToSymmetricNormalization();
		
		long numClasses = labels.getCols();
		ModelBuilder modelBuilder = new GCNBuilder(adjacency, features)
				.config("reg", 0.0005)
				.config("classes", numClasses)
				.config("hidden", numClasses)
				.constant("I", Matrix.eye(numClasses))
				.layer("h{l+1}=relu(h{l}@matrix(features, hidden, reg)+vector(hidden))")
				.rememberAs("0")
				.constant("a", 0.1)
				.futureConstants("b{l}", l->0.5/(l+1), 16)
				.layerRepeat("ch{l}=b{l}*matrix(hidden, hidden)+(1-b{l})*I; h{l+1}=relu((1-a)*(dropout(A, 0.5)@h{l})+a*h{0})@ch{l}", 16)
				.layer("h{l+1}=h{l}@matrix(hidden, classes)+vector(classes)")
				.classify()
				.assertBackwardValidity()
				.print();
		
		ModelTraining trainer = new ModelTraining()
				.setOptimizer(new Adam(0.01))
				.setEpochs(300)
				.setPatience(100)
				.setLoss(new CategoricalCrossEntropy())
				.setValidationLoss(new CategoricalCrossEntropy());
		
		long tic = System.currentTimeMillis();
		Slice nodes = nodes2Ids.getSlice().shuffle(100);
		Model model = modelBuilder.getModel()
				.init(new XavierNormal())
				.train(trainer,
						Tensor.fromRange(0, nodes.size()).asColumn(), 
						labels, nodes.range(0, 0.6), nodes.range(0.6, 0.8));
		
		System.out.println("Training time "+(System.currentTimeMillis()-tic)/1000.);
		Matrix output = model.predict(Tensor.fromRange(0, nodes.size()).asColumn()).get(0).cast(Matrix.class);
		double acc = 0;
		for(Long node : nodes.range(0.8, 1)) {
			Matrix nodeLabels = labels.accessRow(node).asRow();
			Tensor nodeOutput = output.accessRow(node).asRow();
			acc += nodeOutput.argmax()==nodeLabels.argmax()?1:0;
		}
		System.out.println("Acc\t "+acc/nodes.range(0.8, 1).size());
	}
}