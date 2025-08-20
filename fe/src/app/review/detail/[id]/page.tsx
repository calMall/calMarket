import { getReviewDetail } from "@/api/Review";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import Star from "@/components/product/Star";
import Link from "next/link";

export default async function ReviewDetail({ params }: { params: { id: string }} ){
  const { id } =  params;

  try {
    const product = await getReviewDetail(Number(id));

    return (
      <CustomLayout>
        <div className="review-detail">
  <div className="product-info">
    <ContainImage
      src="https://image.rakuten.co.jp/bc7/cabinet/f/f7759176/fw1k1_001.jpg"
      alt="Product Image"
      className="product-image"
    />
    <div className="product-name">
      【公式・新品・送料無料】デスクトップパソコン 一体型 office付き 新品 おすすめ 富士通 FMV Desktop F WF1-K1【FH75-K1ベースモデル】<br />
      23.8型 Windows11 Home Celeron メモリ4GB SSD 256GB Office 搭載モデル RK_WF1K1_A002
    </div>
  </div>

  <div className="review-info">
    <div className="nickname">石田</div>
    <div className="title">デスクトップパソコン</div>
    <div className="star-rating">
      <span>★</span><span>★</span><span>★</span><span>★</span><span>☆</span>
    </div>
    <div className="date">2077年7月7日</div>
    <div className="comment">手ごろな値段でとてもいい！</div>
  </div>

  <div className="buttons">
    <button className="edit-btn">編集</button>
    <button className="delete-btn">削除</button>
  </div>
</div>

        
      </CustomLayout>
    );
  } catch (e) {
    console.error(e);
    return <ErrorComponent />;
  }
}
