typedef enum
{
    EnumDef_Undefined = -1,
    EnumDef_Big = 0x80000000

} EnumDef;

typedef struct structType
{
    EnumDef someEnum;
    long long bigNumber;
} value;
