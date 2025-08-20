"use client";
import { getCart } from "@/api/Cart";
import CartContain from "@/components/cart/CartContain";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import UserStore from "@/store/user";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function Cart() {
  const [carts, setCarts] = useState<cartItem[] | null>(null);
  const router = useRouter();
  const userStore = UserStore();
  const getData = async () => {
    try {
      const data = await getCart();
      setCarts(data.cartItems);
      console.log(data);
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        return router.push("/login");
      }
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
