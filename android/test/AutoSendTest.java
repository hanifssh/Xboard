import com.qwerty.keyboard.AutoSend;

public class AutoSendTest {
    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] a) {
        check("one paste + enter cycle finishes under 200ms", AutoSend.CYCLE_MS < 200);
        check("enter is pressed after the paste within the cycle", AutoSend.ENTER_DELAY_MS > 0 && AutoSend.ENTER_DELAY_MS < AutoSend.CYCLE_MS);
        check("amount 25 runs 25 times", AutoSend.parseAmount("25") == 25);
        check("amount 0 still runs once", AutoSend.parseAmount("0") == 1);
        check("empty amount uses default 10", AutoSend.parseAmount("") == 10);
        check("huge amount is capped", AutoSend.parseAmount("99999999") == AutoSend.AMOUNT_MAX);
        if (failed > 0) System.exit(1);
    }
}
