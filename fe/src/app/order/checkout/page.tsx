import CustomLayout from "@/components/common/CustomLayout";
import CheckoutOrder from "@/components/order/CheckoutOrder";

interface props {
  searchParams: Promise<{ ids: string }>;
}
export default async function CheckoutOrderPage({ searchParams }: props) {
  const { ids } = await searchParams;
  return (
    <CustomLayout>
      <CheckoutOrder ids={ids} />
    </CustomLayout>
  );
}
