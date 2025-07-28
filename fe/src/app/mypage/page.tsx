"use client";

import { myInfo } from "@/api/User";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import Star from "@/components/product/Star";
import UserStore from "@/store/user";
import { dateFormat } from "@/utils/dateFormat";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function Mypage() {
  const userStore = UserStore();
  const [point, setPoint] = useState<null | number>(null);
  const [orders, setOrders] = useState<SimpleOrder[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  useEffect(() => {
    const setData = async () => {
      try {
        const data = await myInfo();
        setPoint(data.point);
        setOrders(data.orders);
        setReviews(data.reviews);
        console.log(data);
      } catch (e) {
        console.log(e);
        alert("エラーが発生しました。");
      }
    };
    setData();
  }, []);
  return (
    <CustomLayout>
      <div className="bb mypage-horizens">
        <h2>ポイント残高</h2>
        {point?.toLocaleString()} ポイント
      </div>
      <div className="bb mypage-horizens">
        <h2>注文履歴</h2>
        {orders.map((order) => (
          <div key={order.id} className="wf simple-order-contain">
            <Link href={`/`} className="rt simple-order-img">
              <ContainImage
                alt="poduct"
                url={newImageSizing(order.imageUrl, 512)}
              />
            </Link>
          </div>
        ))}
      </div>
      <div className="mypage-horizens">
        <h2>「{userStore.userInfo?.nickname}」さんのレビュー</h2>
        {reviews.map((review) => (
          <div key={review.id} className="wf simple-order-contain">
            <Link href={`/`} className="simple-order-img pd-1">
              <div className="review-title ">{review.title}</div>
              <Star score={review.score} />
              <div className="mt-05">{dateFormat(review.createdAt)}</div>
              <div className="review-content mt-05">{review.content} </div>
            </Link>
          </div>
        ))}
      </div>
    </CustomLayout>
  );
}
