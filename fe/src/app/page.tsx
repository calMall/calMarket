import { rakutenRanking } from "@/api/Rakuten";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import MainSlider from "@/components/common/MainSlider";
import ProductItem from "@/components/product/ProductItem";

export default async function Home() {
  try {
    const data = await rakutenRanking();
    return (
      <>
        <MainSlider />
        <CustomLayout>
          <h1>ランキング</h1>
          <div className="ranking-list-contain">
            {data.Items.map((item) => (
              <ProductItem product={item} key={item.Item.itemCode} />
            ))}
          </div>
        </CustomLayout>
      </>
    );
  } catch (e) {
    return <ErrorComponent />;
  }
}
