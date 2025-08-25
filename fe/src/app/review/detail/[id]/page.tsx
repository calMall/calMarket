import { getReviewDetail } from "@/api/Review";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import Star from "@/components/product/Star";
import ReviewDetailBtns from "@/components/review/ReviewDetailBtns";
import ReviewImages from "@/components/review/ReviewImages";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";

export default async function ReviewDetail({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  try {
    const data = await getReviewDetail(Number(id));

    return (
      <CustomLayout>
        <div className="review-item">
          {/* 商品情報 */}
          <Link
            href={`/product/${data.itemCode}`}
            className="flex simple-review-contain mt-2"
          >
            <div className="rt simple-order-img simple-img">
              <ContainImage
                alt="poduct"
                url={newImageSizing(data.imageUrls[0], 512)}
              />
            </div>
            <div className="review-itemname">{data.itemName}</div>
          </Link>

          {/* レビュー内容 */}
          <div className="simple-review-contain">
            <div>
              <div className="flex ac gap-05">
                <div className="profile-img rt">
                  <ContainImage url="/profile.png" alt="profile" />
                </div>
                <span>{data.userNickname}</span>
              </div>
            </div>
            <div className="review-title-in-product mt-1">{data.title}</div>
            <Star className="mt-1 " score={data.rating} />
            <div className="date-font">
              {new Date(data.createdAt).toLocaleDateString("ja-JP", {
                year: "numeric",
                month: "long",
                day: "numeric",
              })}
            </div>
            <ReviewImages imageList={data.imageList} />
            <div className="comment mt-1">{data.comment}</div>
          </div>

          {/* 編集・削除ボタン */}
          <ReviewDetailBtns itemCode={data.itemCode} reviewId={data.reviewId} />
        </div>
      </CustomLayout>
    );
  } catch (e) {
    console.error(e);
    return <ErrorComponent />;
  }
}
