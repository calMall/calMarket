"use client";

import { getOrderDetail, PostCancelOrder, PostRefundOrder } from "@/api/Order";
import CustomLayout from "@/components/common/CustomLayout";
import LoadingOrderBox from "@/components/order/LoadingOrderBox";
import OrderStatus from "@/components/order/OrderStatus";
import UserStore from "@/store/user";
import { dateFormat } from "@/utils/dateFormat";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const boxStyle = {
  border: "1px solid #ccc",
  borderRadius: "6px",
  padding: "1rem",
  backgroundColor: "#f9f9f9",
  lineHeight: "1.6",
  marginBottom: "1.5rem",
};

export default function OrderDetail({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const [orderInfo, setOrderInfo] = useState<OrderInfo | null>(null);
  const [totalPrice, setTotalPrice] = useState(0);
  const [loading, setLoading] = useState(true);
  const [totalCount, setTotalCount] = useState(0);
  const userStore = UserStore();
  const router = useRouter();

  const onCancelOrder = async () => {
    if (window.confirm("注文をキャンセルしますか？")) {
      try {
        const { id } = await params;
        const res = await PostCancelOrder(Number(id));
        console.log(res);
      } catch (e: any) {
        if (e.status === 401) {
          alert("ログインが必要です。ログインページに移動します。");
          userStore.logout();
          return router.push("/login");
        }
        alert(e.message.split(":")[1].trim());
        router.back();
      }
    }
  };
  const onRefundOrder = async () => {
    if (window.confirm("注文を払い戻しますか？")) {
      try {
        const { id } = await params;
        const res = await PostRefundOrder(Number(id));
        console.log(res);
      } catch (e: any) {
        if (e.status === 401) {
          alert("ログインが必要です。ログインページに移動します。");
          userStore.logout();
          return router.push("/login");
        }
        alert(e.message.split(":")[1].trim());
        return router.back();
      }
    }
  };

  const fetchData = async () => {
    try {
      const { id } = await params;
      const data = await getOrderDetail(Number(id));
      if (data.message === "success") {
        setOrderInfo(data.order);
        const total = data.order.orderItems.reduce((acc, item) => {
          return acc + item.price * item.quantity;
        }, 0);
        setTotalPrice(total);
        const cnt = data.order.orderItems.reduce((acc, item) => {
          return acc + item.quantity;
        }, 0);
        setTotalCount(cnt);
        setLoading(false);
      }
      console.log(data);
    } catch (e: any) {
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        return router.push("/login");
      }
      alert(e.message);
      return router.back();
    }
  };

  useEffect(() => {
    fetchData();
  }, []);
  return (
    <CustomLayout>
      <h2>注文の詳細</h2>
      {loading ? (
        [1, 2, 3].map((_) => <LoadingOrderBox key={_} />)
      ) : (
        <>
          {orderInfo &&
            orderInfo.orderItems.map((item) => (
              <div key={item.itemCode} className="order-item bb mb-1 pb-1">
                <Link
                  href={`/product/${item.itemCode}`}
                  className="hover-anime"
                  style={{
                    display: "flex",
                    alignItems: "center",
                    marginBottom: "1rem",
                  }}
                >
                  <Image
                    alt="product"
                    src={item.imageList[0]}
                    width={128}
                    height={128}
                  />

                  <div
                    className="product-info"
                    style={{
                      fontWeight: "bold",
                      marginLeft: "1rem",
                      fontSize: "0.9rem",
                      lineHeight: "1.5",
                    }}
                  >
                    <p>{item.itemName}</p>
                  </div>
                </Link>
                <div className="wf flex jb">
                  {orderInfo && orderInfo.status === "DELIVERED" && (
                    <Link
                      className="review-write-btn"
                      href={`/review/write/${item.itemCode}`}
                    >
                      レビューを書く
                    </Link>
                  )}
                  {orderInfo.status === "PENDING" ? (
                    <button className="red-hover" onClick={onCancelOrder}>
                      注文キャンセル
                    </button>
                  ) : (
                    orderInfo.status === "DELIVERED" && (
                      <button className="red-hover" onClick={onRefundOrder}>
                        払い戻し
                      </button>
                    )
                  )}
                </div>
              </div>
            ))}

          <div className="order-date" style={{ marginBottom: "1rem" }}>
            注文日：{orderInfo && dateFormat(orderInfo.orderDate)}
          </div>
          {orderInfo && (
            <OrderStatus status={orderInfo.status as OrderStatusUI} />
          )}
          <div className="delivered-message bb">
            {orderInfo && orderInfo.status === "DELIVERED"
              ? "配達が完了しました。"
              : orderInfo && orderInfo.status === "CANCELLED"
              ? "キャンセルされた注文です。"
              : orderInfo && orderInfo.status === "REFUNDED"
              ? "払い戻しが完了しました。"
              : ""}
          </div>
          <h3
            style={{
              fontWeight: "bold",
              marginTop: "1rem",
              marginBottom: "0.5rem",
            }}
          >
            お届け先
          </h3>
          <div className="box receipt-box" style={boxStyle}>
            <h4>{orderInfo && orderInfo.deliveryAddress.slice(0, 8)}</h4>
            <div>{orderInfo && orderInfo.deliveryAddress.slice(8)}</div>
          </div>

          <h3 style={{ marginTop: "1rem", marginBottom: "1rem" }}>領収書</h3>
          <div className="box receipt-box" style={boxStyle}>
            <div>商品の数量：{totalCount}</div>
            <div>商品の小計：￥{totalPrice.toLocaleString()}</div>
            <div>配送料・手数料：￥0</div>
            <h4>注文合計：￥{totalPrice.toLocaleString()}</h4>
          </div>
        </>
      )}
    </CustomLayout>
  );
}
