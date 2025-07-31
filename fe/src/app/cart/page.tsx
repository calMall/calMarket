"use client";
import { getCart } from "@/api/Cart";
import CartContain from "@/components/cart/CartContain";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import { useEffect, useState } from "react";

export default function Cart() {
  const getData = async () => {
    try {
      const data = await getCart();
      console.log(data);
    } catch (e: any) {
      console.log(e);
      return <ErrorComponent />;
    }
  };
  const [reviews, setReviews] = useState<CartListResponseDto | null>(null);
  useEffect(() => {});

  return (
    <CustomLayout>
      {/* <CartContain initCartList={data.cartItems} /> */}
      <div></div>
    </CustomLayout>
  );
}
