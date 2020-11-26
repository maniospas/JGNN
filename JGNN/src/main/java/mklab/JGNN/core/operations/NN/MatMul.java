package mklab.JGNN.core.operations.NN;

import java.util.List;

import mklab.JGNN.core.operations.NNOperation;
import mklab.JGNN.core.primitives.Matrix;
import mklab.JGNN.core.primitives.Tensor;

public class MatMul extends NNOperation {
	@Override
	protected Tensor forward(List<Tensor> inputs) {
		Matrix W = (Matrix) inputs.get(0);
		Matrix H = (Matrix) inputs.get(1);
		return W.matmul(H);
	}

	@Override
	protected Tensor partial(int inputId, List<Tensor> inputs, Tensor output, Tensor error) {
		Matrix errorMatrix = (Matrix)error;
		Matrix W = (Matrix) inputs.get(0);
		Matrix H = (Matrix) inputs.get(1);
		if(inputId==0)
			errorMatrix = errorMatrix.matmul(H, false, true);
		else if(inputId==1) 
			errorMatrix = W.matmul(errorMatrix, true, false);
		return errorMatrix;
	}
}
