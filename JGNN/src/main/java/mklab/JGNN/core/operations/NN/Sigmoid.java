package mklab.JGNN.core.operations.NN;

import java.util.List;

import mklab.JGNN.core.operations.NNOperation;
import mklab.JGNN.core.primitives.Tensor;
import mklab.JGNN.core.util.Loss;

public class Sigmoid extends NNOperation {
	@Override
	protected Tensor forward(List<Tensor> inputs) {
		return Loss.sigmoid(inputs.get(0));
	}
	@Override
	protected Tensor partial(int inputId, List<Tensor> inputs, Tensor output, Tensor error) {
		return Loss.sigmoidDerivative(inputs.get(0)).selfMultiply(error);
	}
}