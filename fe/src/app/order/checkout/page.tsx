import { getCheckout } from "@/api/Cart";
interface props {
  searchParams: Promise<{ ids: string }>;
}

export default async function CheckoutOrder({ searchParams }: props) {
  const { ids } = await searchParams;
  console.log(ids);
  const data = await getCheckout(ids.split(",").map((id) => Number(id)));
  console.log(data);
  return (
    <div>
      <div></div>
    </div>
  );
}
