// Nicht übersetzbares Konstrukt (unsafe) neben normalem Code (M2.3).
public class UnsafeBits
{
    public int Plain(int a)
    {
        return a + 1;
    }

    public int Peek(int value)
    {
        int result;
        unsafe
        {
            int* p = &value;
            result = *p;
        }
        return result;
    }
}
