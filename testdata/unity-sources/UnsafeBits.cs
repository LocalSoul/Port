// Nicht übersetzbares Konstrukt (unsafe/fixed) neben normalem Code (M2.3). Der fixed-Block bleibt im
// Release-Build erhalten (Pinning), ein einfaches *(&x) würde der Compiler wegoptimieren.
public class UnsafeBits
{
    public int Plain(int a)
    {
        return a + 1;
    }

    public int First(int[] values)
    {
        int result = 0;
        unsafe
        {
            fixed (int* p = values)
            {
                result = p[0];
            }
        }
        return result + Plain(result);
    }
}
