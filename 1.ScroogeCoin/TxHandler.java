import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    private UTXOPool currutxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.currutxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        ArrayList<UTXO> txUTXO = new ArrayList<UTXO>();
        double inputValue = 0;
        double outputValue = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input txInput = txInputs.get(i);
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            Transaction.Output outputObj = currutxoPool.getTxOutput(utxo);
            // 1. all outputs claimed are in the current UTXO pool,
            if (!currutxoPool.contains(utxo))
                return false;
            // 2. verify signature
            byte[] message = tx.getRawDataToSign(i);
            if(!Crypto.verifySignature(outputObj.address, message, txInput.signature))
                return false;
            //3. no UTXO is claimed multiple times
            if (txUTXO.contains(utxo))
                return false;
            txUTXO.add(utxo);
            inputValue += outputObj.value;
        }
        for (Transaction.Output output : txOutputs) {
            // 4. all of the output values are non-negative
            if (output.value < 0)
                return false;
            outputValue += output.value;
        }
        //5. the sum of all input values is greater than or equal to the sum of its output values; and false otherwise.
        if (inputValue < outputValue)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> accTransactions = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                accTransactions.add(tx);
                ArrayList<Transaction.Input> txInputs = tx.getInputs();
                ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
                //remove used inputs
                for (Transaction.Input txInput : txInputs) {
                    UTXO spentUTXO = new UTXO(txInput.prevTxHash, txInput.outputIndex);
                    currutxoPool.removeUTXO(spentUTXO);
                }
                //add new outputs
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output newOutput = txOutputs.get(i);
                    UTXO newUTXO = new UTXO(tx.getHash(),i);
                    currutxoPool.addUTXO(newUTXO, newOutput);
                }
            }
        }
        return accTransactions.toArray(new Transaction[accTransactions.size()]);
    }

}
