"use client";
import { getCart } from "@/api/Cart";
import { getOrderList } from "@/api/Order";
import CartContain from "@/components/cart/CartContain";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import OrderedItem from "@/components/order/OrderedItem";
import UserStore from "@/store/user";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function OrderList() {
  const [orders, setOrders] = useState<OrderInfoOnList[] | null>(null);
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);
  const userStore = UserStore();
  const getData = async () => {
    try {
      const data = await getOrderList();
      setOrders(data.orders);
      setIsLoading(false);
      console.log(data);
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        return router.push("/login");
      }
      alert("エラーが発生しました。");
      return router.back();
    }
  };
  useEffect(() => {
    getData();
  }, []);

  return (
    <CustomLayout>
      <h2>注文履歴</h2>
      <div className="bb wf jb flex pb-1 mb-1" />

      {isLoading ? (
        <div>Loading...</div>
      ) : orders && orders.length > 0 ? (
        <div className="mt-1 flex flex-col gap-1">
          {orders.map((order, idx) => (
            <OrderedItem key={idx} item={order} />
          ))}
        </div>
      ) : (
        <div>注文履歴がありません</div>
      )}
    </CustomLayout>
  );
}
