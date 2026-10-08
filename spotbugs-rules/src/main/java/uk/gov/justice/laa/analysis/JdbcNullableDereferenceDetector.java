package uk.gov.justice.laa.analysis;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import org.apache.bcel.classfile.Code;

import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.INVOKEINTERFACE;
import static org.apache.bcel.Const.INVOKEVIRTUAL;

/**
 * Detects direct dereferences of nullable values returned by java.sql.ResultSet.
 * ResultSets getters returning primitive types are guaranteed to return a value,
 * but getters returning objects may return null so they are excluded.
 * This detector currently covers DIRECT dereferences only.
 * This detector currently does not cover dereferences through local variables as these
 * are already covered by the FindBugs NP_NULL_ON_SOME_PATH detector.
 */
public class JdbcNullableDereferenceDetector extends BytecodeScanningDetector {

    private static final String JDBC_RESULT_SET_CLASS = "java/sql/ResultSet";
    private static final String JDBC_NULL_DEREFERENCE = "JDBC_NULL_DEREFERENCE";
    private static final String JDBC_METHOD_SIGNATURE = "(Ljava/lang/String;";

    private final BugReporter reporter;
    private boolean previousWasTimestampGetter;

    public JdbcNullableDereferenceDetector(BugReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void visit(Code code) {
        previousWasTimestampGetter = false;
        super.visit(code);
    }

    @Override
    public void sawOpcode(int seen) {
        boolean nullableJdbcGetter = isNullableResultSetGetter(seen);
        boolean dereference = seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE;

        if (previousWasTimestampGetter && dereference) {
            BugInstance bug = new BugInstance(this, JDBC_NULL_DEREFERENCE, NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this)
                    .addString(getDottedClassName() + "." + (getMethodName()));

            reporter.reportBug(bug);
        }
        if (seen == CHECKCAST && previousWasTimestampGetter) {
            return;
        }
        previousWasTimestampGetter = nullableJdbcGetter;
    }

    private boolean isNullableResultSetGetter(final int seen) {
        if (seen != INVOKEINTERFACE) {
            return false;
        }

        if (!JDBC_RESULT_SET_CLASS.equals(getClassConstantOperand())) {
            return false;
        }

        String methodName = getNameConstantOperand();
        String methodSignature = getSigConstantOperand();

        if (!methodName.startsWith("get")) {
            return false;
        }

        boolean columnGetter = methodSignature.startsWith("(I") || methodSignature.startsWith(JDBC_METHOD_SIGNATURE);
        if (!columnGetter) {
            return false;
        }

        String returnType = methodSignature.substring(methodSignature.indexOf(')') + 1);

        return returnType.startsWith("L") || returnType.startsWith("[");
    }
}
