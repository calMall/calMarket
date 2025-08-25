"use client";

import { getCart } from "@/api/Cart";
import { getMyInfo } from "@/api/User";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import Star from "@/components/product/Star";
import LoadingBox from "@/components/user/LoadingBox";
import UserStore from "@/store/user";
import { dateFormat } from "@/utils/dateFormat";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function Mypage() {
  const userStore = UserStore();
  const router = useRouter();
  const [orders, setOrders] = useState<SimpleOrder[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const setData = async () => {
    try {
      const data = await getMyInfo();

      setOrders(data.orders ? data.orders : []);
      setReviews(data.reviews ? data.reviews : []);
      setIsLoading(false);
    } catch (e: any) {
      console.log(e);
      if (e.status === 401) {
        alert("ログインが必要です。ログインページに移動します。");
        userStore.logout();
        router.push("/login");
      } else {
        alert("レビューの投稿中にエラーが発生しました。");
      }
    }
  };
  useEffect(() => {
    setData();
  }, []);
  return (
    <CustomLayout>
      <div className="bb mypage-horizens">
        <div className="flex jb">
          <h2>注文履歴</h2>
          <Link className="flex ac color-deep-dark-main" href={"/order/list"}>
            もっと見る
          </Link>
        </div>
        {isLoading ? (
          <div className="wf simple-order-contain">
            {[1, 2, 3, 4].map((_) => (
              <LoadingBox key={_} />
            ))}
          </div>
        ) : orders.length === 0 ? (
          <div>注文履歴がありません</div>
        ) : (
          <div className="wf simple-order-contain">
            {orders.slice(0, 4).map((order) => (
              <div className="wf" key={order.id}>
                <Link href={`/order/list/detail/${order.id}`}>
                  <div className="rt simple-order-img wf hover-anime">
                    <ContainImage
                      alt="product"
                      url={newImageSizing(order.imageUrl, 256)}
                    />
                  </div>
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>

      <div>
        <div className="mypage-horizens">
          {!isLoading && (
            <div className="flex jb">
              <h2>「{userStore.userInfo?.nickname}」さんのレビュー</h2>
              <Link
                className="flex ac color-deep-dark-main"
                href={"/review/list"}
              >
                もっと見る
              </Link>
            </div>
          )}

          {isLoading ? (
            <div className="wf simple-order-contain">
              {[1, 2, 3, 4].map((_) => (
                <LoadingBox key={_} />
              ))}
            </div>
          ) : reviews.length === 0 ? (
            <div>レビューがありません</div>
          ) : (
            <div className="wf simple-order-contain">
              {reviews.slice(0, 4).map((review) => (
                <Link
                  key={review.id}
                  href={`/review/detail/${review.id}`}
                  className="simple-order-img pd-1 hover-anime"
                >
                  <div className="review-title">{review.title}</div>
                  <Star score={review.score} />
                  <div className="mt-05 review-date">
                    {dateFormat(review.createdAt)}
                  </div>
                  <div className="review-content mt-05">{review.content} </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </CustomLayout>
  );
}
