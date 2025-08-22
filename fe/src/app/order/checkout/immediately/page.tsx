import CheckoutOrderImmediately from "@/components/order/CheckoutOrderImmediately";

interface props {
  searchParams: Promise<{ itemCode: string; quantity: number }>;
}

export default async function CheckoutOrderImmediatelyPage({
  searchParams,
}: props) {
  // カート情報ロード関数
  const { itemCode, quantity } = await searchParams;

  return <CheckoutOrderImmediately itemCode={itemCode} quantity={quantity} />;
}
