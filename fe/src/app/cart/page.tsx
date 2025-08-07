"use client";
import { getCart } from "@/api/Cart";
import CartContain from "@/components/cart/CartContain";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import { useEffect, useState } from "react";

export default function Cart() {
  const [carts, setCarts] = useState<cartItem[] | null>(null);
  const getData = async () => {
    try {
      const data = await getCart();
      setCarts(data.cartItems);
      console.log(data);
    } catch (e: any) {
      console.log(e);
      return <ErrorComponent />;
    }
  };
  useEffect(() => {
    getData();
  }, []);

  return (
    <CustomLayout>{carts && <CartContain initCartList={carts} />}</CustomLayout>
  );
}
