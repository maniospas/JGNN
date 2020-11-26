package mklab.JGNN.core.operations.NN;

import java.util.List;
import java.util.Map.Entry;

import mklab.JGNN.core.operations.NNOperation;
import mklab.JGNN.core.primitives.Matrix;
import mklab.JGNN.core.primitives.Tensor;
import mklab.JGNN.core.primitives.tensor.DenseTensor;

public class Sum extends NNOperation {
	@Override
	protected Tensor forward(List<Tensor> inputs) {
		if(inputs.get(0) instanceof Matrix) {
			Matrix matrix = (Matrix) inputs.get(0);
			Tensor ret = new DenseTensor(matrix.getRows());
			for(Entry<Long, Long> entry : matrix.getNonZeroEntries()) {
				long row = entry.getKey();
				long col = entry.getValue();
				ret.put(row, ret.get(row) + matrix.get(row, col));
			}
			return ret;
		}
		else {
			double sum = 0;
			for(long i : inputs.get(0).getNonZeroElements())
				sum += inputs.get(0).get(i);
			return Tensor.fromDouble(sum);
		}
	}
	@Override
	protected Tensor partial(int inputId, List<Tensor> inputs, Tensor output, Tensor error) {
		if(inputs.get(0) instanceof Matrix) {
			Matrix matrix = (Matrix) inputs.get(0);
			Matrix ret = (Matrix) matrix.zeroCopy().setToOnes();
			for(Entry<Long, Long> entry : matrix.getNonZeroEntries()) {
				long row = entry.getKey();
				long col = entry.getValue();
				ret.put(row, col, ret.get(row, col)*error.get(row));
			}
			return ret;
		}
		else {
			double errorValue = error.toDouble();
			return inputs.get(0).zeroCopy().setToOnes().multiply(errorValue);
		}
	}
}